const db = require('../config/db');
const authService = require('../services/authService');
const jwt = require('jsonwebtoken');
const notificationService = require('../services/notificationService');
const crypto = require('crypto');

/**
 * Registers a new user and generates an OTP.
 */
const signup = async (req, res) => {
  const { full_name, email, phone, password, role, referral_code } = req.body;
  const userRole = (role || 'CUSTOMER').toUpperCase();

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const passwordHash = await authService.hashPassword(password);

    // Generate unique referral code for this user
    const userReferralCode = crypto.randomBytes(4).toString('hex').toUpperCase();

    // Check if referred by someone
    let referredByUserId = null;
    if (referral_code) {
        const refRes = await client.query("SELECT id FROM users WHERE referral_code = $1", [referral_code.toUpperCase()]);
        if (refRes.rows.length > 0) {
            referredByUserId = refRes.rows[0].id;
        }
    }

    // 1. Create User
    const userRes = await client.query(
      "INSERT INTO users (full_name, email, phone, password_hash, role, referral_code, referred_by_user_id) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id, email, role",
      [full_name, email, phone, passwordHash, userRole, userReferralCode, referredByUserId]
    );
    const user = userRes.rows[0];

    // 2. Initialize Role-specific data
    if (userRole === 'FULFILLER') {
      const fulfillerRes = await client.query(
        "INSERT INTO fulfillers (user_id) VALUES ($1) ON CONFLICT (user_id) DO UPDATE SET user_id = EXCLUDED.user_id RETURNING id",
        [user.id]
      );
      const fulfillerId = fulfillerRes.rows[0].id;

      await client.query(
        "INSERT INTO wallets (owner_id, owner_type) VALUES ($1, 'FULFILLER')",
        [fulfillerId]
      );
    } else {
      await client.query(
        "INSERT INTO wallets (owner_id, owner_type) VALUES ($1, 'USER')",
        [user.id]
      );
    }

    // 3. Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000); // 10 minutes

    await client.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    await client.query('COMMIT');
    client.release(); // Important: Release before post-transaction async tasks

    // 4. Send email with OTP (Asynchronous, no longer blocks)
    notificationService.sendOTPEmail(user.id, email, otp).catch(e => {
        console.error('Initial OTP send failed:', e.message);
    });

    res.status(201).json({
      message: 'User registered. Please verify your email.',
      userId: user.id,
      email: user.email,
      role: user.role
    });
  } catch (error) {
    if (client) {
        try { await client.query('ROLLBACK'); } catch (e) {}
    }
    console.error('SIGNUP ERROR:', error);

    if (error.code === '23505') {
      const field = error.detail.includes('email') ? 'Email address' : 'Phone number';
      return res.status(400).json({ message: `${field} is already registered. Please try logging in instead.` });
    }

    res.status(500).json({ message: 'A server error occurred during registration: ' + error.message });
  } finally {
    if (client) client.release();
  }
};

/**
 * Verifies email using the OTP.
 */
const verifyEmail = async (req, res) => {
  const { email, otp } = req.body;

  try {
    const { rows } = await db.query(
      `SELECT ov.*, u.id as user_id
       FROM otp_verifications ov
       JOIN users u ON u.id = ov.user_id
       WHERE u.email = $1 AND ov.otp_code = $2 AND ov.expires_at > CURRENT_TIMESTAMP`,
      [email, otp]
    );

    if (rows.length === 0) {
      return res.status(400).json({ message: 'Invalid or expired verification code. Please request a new one.' });
    }

    const userId = rows[0].user_id;

    const userRes = await db.query("SELECT * FROM users WHERE id = $1", [userId]);
    const user = userRes.rows[0];

    await db.query(
      "UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE id = $1",
      [userId]
    );

    // Delete used OTP
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [userId]);

    // Send Welcome Email
    notificationService.sendWelcomeEmail(userId).catch(e => console.error('Welcome email failed:', e.message));

    // Generate tokens after verification
    const tokens = authService.generateTokens(user);

    // Register Session
    await db.query(
        "INSERT INTO user_sessions (user_id, refresh_token, device_name, ip_address) VALUES ($1, $2, $3, $4)",
        [userId, tokens.refreshToken, req.headers['user-agent'], req.ip]
    );

    res.status(200).json({
        message: 'Verification success',
        userId: user.id,
        email: user.email,
        full_name: user.full_name,
        phone: user.phone,
        role: user.role,
        referral_code: user.referral_code,
        ...tokens
    });
  } catch (error) {
    console.error('Verification Error:', error);
    res.status(500).json({ message: 'Verification failed. Please try again later.' });
  }
};

/**
 * Authenticates user and returns tokens.
 */
const login = async (req, res) => {
  const { email, password } = req.body;

  try {
    const { rows } = await db.query("SELECT * FROM users WHERE email = $1", [email]);
    if (rows.length === 0) return res.status(401).json({ message: 'Invalid email or password.' });

    const user = rows[0];
    const isMatch = await authService.comparePassword(password, user.password_hash);
    if (!isMatch) return res.status(401).json({ message: 'Invalid email or password.' });

    const tokens = authService.generateTokens(user);

    // Register Session
    await db.query(
        "INSERT INTO user_sessions (user_id, refresh_token, device_name, ip_address) VALUES ($1, $2, $3, $4)",
        [user.id, tokens.refreshToken, req.headers['user-agent'], req.ip]
    );

    res.status(200).json({
      message: 'Login successful',
      userId: user.id,
      email: user.email,
      full_name: user.full_name,
      phone: user.phone,
      role: user.role,
      referral_code: user.referral_code,
      ...tokens
    });
  } catch (error) {
    console.error('Login Error:', error);
    res.status(500).json({ message: 'Login failed due to a server error.' });
  }
};

/**
 * Refreshes the access token using a refresh token.
 */
const refreshToken = (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) return res.status(401).json({ message: 'Session expired. Please login again.' });

  jwt.verify(refreshToken, authService.REFRESH_TOKEN_SECRET, (err, user) => {
    if (err) return res.status(403).json({ message: 'Invalid session. Please login again.' });

    // Generate new tokens
    const tokens = authService.generateTokens(user);
    res.json(tokens);
  });
};

/**
 * Updates the user's FCM token.
 */
const updateFCMToken = async (req, res) => {
  const userId = req.user.id;
  const { token } = req.body;
  try {
    const fcmService = require('../services/fcmService');
    await fcmService.saveToken(userId, token);
    res.status(200).json({ message: 'Notifications activated.' });
  } catch (error) {
    res.status(500).json({ message: 'Failed to activate notifications.' });
  }
};

/**
 * Resends a verification OTP with rate limiting.
 */
const resendOtp = async (req, res) => {
  const { email } = req.body;

  try {
    const { rows: userRows } = await db.query("SELECT id FROM users WHERE email = $1", [email]);
    if (userRows.length === 0) return res.status(404).json({ message: 'Account not found' });
    const userId = userRows[0].id;

    // Hourly Rate Limit check (Technical Spec 6.3)
    const { rows: attempts } = await db.query(
        "SELECT COUNT(*) FROM otp_verifications WHERE user_id = $1 AND created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour'",
        [userId]
    );

    if (parseInt(attempts[0].count) >= 5) {
        return res.status(429).json({ message: 'RATE_LIMITED', error: 'Too many attempts. Please try again in 1 hour.' });
    }

    // Invalidate previous OTPs
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [userId]);

    // Generate new OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000);

    await db.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [userId, otp, expiresAt]
    );

    notificationService.sendOTPEmail(userId, email, otp);

    res.status(200).json({ message: 'New verification code sent.' });
  } catch (error) {
    res.status(500).json({ message: 'Failed to resend code: ' + error.message });
  }
};

module.exports = {
  signup,
  verifyEmail,
  resendOtp,
  login,
  refreshToken,
  updateFCMToken
};
