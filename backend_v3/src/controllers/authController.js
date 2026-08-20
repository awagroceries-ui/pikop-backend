const db = require('../config/db');
const authService = require('../services/authService');
const emailService = require('../services/emailService');
const crypto = require('crypto');

/**
 * Handles user registration.
 */
const signup = async (req, res) => {
  const { full_name, email, phone, password, role, referral_code } = req.body;
  const userRole = (role || 'CUSTOMER').toUpperCase();

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Hash Password
    const passwordHash = await authService.hashPassword(password);

    // 2. Generate unique referral code
    const userReferralCode = crypto.randomBytes(4).toString('hex').toUpperCase();

    // 3. Create User
    const userRes = await client.query(
      `INSERT INTO users (full_name, email, phone, password_hash, role, referral_code)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, email, role`,
      [full_name, email, phone, passwordHash, userRole, userReferralCode]
    );
    const user = userRes.rows[0];

    // 4. Generate OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000); // 10 mins

    await client.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    await client.query('COMMIT');

    // 5. Send OTP Email (Async)
    const subject = `Your Pikop Verification Code: ${otp}`;
    const html = `<h2>Welcome to Pikop</h2><p>Your verification code is: <b>${otp}</b></p>`;
    emailService.sendMail(email, subject, html).catch(err => console.error('[Auth] Initial OTP fail:', err.message));

    res.status(201).json({
      success: true,
      message: 'User registered. Please verify your email.',
      userId: user.id,
      email: user.email,
      role: user.role
    });

  } catch (error) {
    await client.query('ROLLBACK');
    if (error.code === '23505') {
      return res.status(400).json({ success: false, message: 'Email or phone already registered' });
    }
    throw error; // Let global handler catch it
  } finally {
    client.release();
  }
};

/**
 * Verifies email with OTP.
 */
const verifyEmail = async (req, res) => {
  const { email, otp } = req.body;
  const masterOtp = process.env.MASTER_OTP;

  try {
    let user;

    // Check Master OTP Bypass
    if (masterOtp && otp.toString().trim() === masterOtp.toString().trim()) {
        const userRes = await db.query("SELECT * FROM users WHERE email = $1", [email]);
        if (userRes.rows.length === 0) return res.status(404).json({ success: false, message: 'Account not found' });
        user = userRes.rows[0];
    } else {
        const { rows } = await db.query(
            `SELECT ov.*, u.*
             FROM otp_verifications ov
             JOIN users u ON u.id = ov.user_id
             WHERE u.email = $1 AND ov.otp_code = $2 AND ov.expires_at > CURRENT_TIMESTAMP`,
            [email, otp]
        );

        if (rows.length === 0) {
            return res.status(400).json({ success: false, message: 'Invalid or expired code' });
        }
        user = rows[0];
    }

    // Mark as verified
    await db.query("UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE id = $1", [user.id]);
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [user.id]);

    const tokens = authService.generateTokens(user);

    // Register Session
    await db.query(
        "INSERT INTO user_sessions (user_id, refresh_token, ip_address) VALUES ($1, $2, $3)",
        [user.id, tokens.refreshToken, req.ip]
    );

    res.status(200).json({
        success: true,
        message: 'Email verified successfully',
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: user.id,
        email: user.email,
        full_name: user.full_name,
        role: user.role,
        referral_code: user.referral_code
    });

  } catch (error) {
    throw error;
  }
};

/**
 * Handles user login.
 */
const login = async (req, res) => {
  const { email, password } = req.body;

  try {
    const { rows } = await db.query("SELECT * FROM users WHERE email = $1", [email]);
    if (rows.length === 0) return res.status(401).json({ success: false, message: 'Invalid credentials' });

    const user = rows[0];
    const isMatch = await authService.comparePassword(password, user.password_hash);
    if (!isMatch) return res.status(401).json({ success: false, message: 'Invalid credentials' });

    if (!user.email_verified_at) {
        return res.status(403).json({ success: false, message: 'ACCOUNT_UNVERIFIED', email: user.email, role: user.role });
    }

    const tokens = authService.generateTokens(user);

    await db.query(
        "INSERT INTO user_sessions (user_id, refresh_token, ip_address) VALUES ($1, $2, $3)",
        [user.id, tokens.refreshToken, req.ip]
    );

    res.status(200).json({
        success: true,
        message: 'Login successful',
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: user.id,
        email: user.email,
        full_name: user.full_name,
        phone: user.phone,
        role: user.role,
        referral_code: user.referral_code
    });
  } catch (error) {
    throw error;
  }
};

/**
 * Resends OTP to user.
 */
const resendOtp = async (req, res) => {
  const { email } = req.body;

  try {
    const userRes = await db.query("SELECT id FROM users WHERE email = $1", [email]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Account not found' });
    }
    const user = userRes.rows[0];

    // Clear old OTPs
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [user.id]);

    // Generate new OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000);

    await db.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    // Send Email
    const subject = `Your New Pikop Verification Code: ${otp}`;
    const html = `<h2>Verify Your Account</h2><p>Your new code is: <b>${otp}</b></p>`;
    emailService.sendMail(email, subject, html).catch(err => console.error('[Auth] Resend fail:', err.message));

    res.status(200).json({ success: true, message: 'Verification code resent successfully' });
  } catch (error) {
    throw error;
  }
};

/**
 * Updates the user's FCM push token.
 */
const updateFCMToken = async (req, res) => {
    const { token } = req.body;
    const userId = req.user.id;

    try {
        await db.query(
            `INSERT INTO user_fcm_tokens (user_id, token, updated_at)
             VALUES ($1, $2, CURRENT_TIMESTAMP)
             ON CONFLICT (user_id) DO UPDATE SET token = $2, updated_at = CURRENT_TIMESTAMP`,
            [userId, token]
        );
        res.status(200).json({ success: true, message: 'FCM token updated' });
    } catch (error) {
        throw error;
    }
};

/**
 * Refreshes the access token using a valid refresh token.
 */
const refresh = async (req, res) => {
    const { refreshToken } = req.body;
    if (!refreshToken) return res.status(401).json({ success: false, message: 'Refresh token required' });

    try {
        const { rows } = await db.query(
            "SELECT u.* FROM user_sessions s JOIN users u ON u.id = s.user_id WHERE s.refresh_token = $1 AND s.revoked = false",
            [refreshToken]
        );

        if (rows.length === 0) {
            return res.status(401).json({ success: false, message: 'Invalid or revoked refresh token' });
        }

        const user = rows[0];
        const tokens = authService.generateTokens(user);

        // Update session with new refresh token (Rotate)
        await db.query(
            "UPDATE user_sessions SET refresh_token = $1, last_active = CURRENT_TIMESTAMP WHERE refresh_token = $2",
            [tokens.refreshToken, refreshToken]
        );

        res.status(200).json({
            success: true,
            accessToken: tokens.accessToken,
            refreshToken: tokens.refreshToken
        });
    } catch (error) {
        throw error;
    }
};

module.exports = {
  signup,
  verifyEmail,
  login,
  resendOtp,
  refresh,
  updateFCMToken
};
