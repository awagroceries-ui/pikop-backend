const db = require('../config/db');
const authService = require('../services/authService');
const jwt = require('jsonwebtoken');
const notificationService = require('../services/notificationService');

/**
 * Registers a new user and generates an OTP.
 */
const signup = async (req, res) => {
  const { full_name, email, phone, password, role } = req.body;
  const userRole = (role || 'CUSTOMER').toUpperCase();

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const passwordHash = await authService.hashPassword(password);

    // Explicit Column check
    const userRes = await client.query(
      "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES ($1, $2, $3, $4, $5) RETURNING id, email, role",
      [full_name, email, phone, passwordHash, userRole]
    );
    const user = userRes.rows[0];

    // If Fulfiller, initialize profile and wallet
    if (userRole === 'FULFILLER') {
      const fulfillerRes = await client.query(
        "INSERT INTO fulfillers (user_id) VALUES ($1) RETURNING id",
        [user.id]
      );
      const fulfillerId = fulfillerRes.rows[0].id;

      await client.query(
        "INSERT INTO wallets (owner_id, owner_type) VALUES ($1, 'FULFILLER')",
        [fulfillerId]
      );
    } else {
      // Initialize Customer Wallet
      await client.query(
        "INSERT INTO wallets (owner_id, owner_type) VALUES ($1, 'USER')",
        [user.id]
      );
    }

    // Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000); // 10 minutes

    await client.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    await client.query('COMMIT');

    // Send email with OTP (Background)
    notificationService.sendOTPEmail(user.id, email, otp).catch(e => {
        console.error('Initial OTP send failed:', e.message);
    });

    const tokens = authService.generateTokens(user);

    res.status(201).json({
      message: 'User registered. Please verify your email.',
      userId: user.id,
      email: user.email,
      role: user.role,
      ...tokens
    });
  } catch (error) {
    if (client) await client.query('ROLLBACK');
    console.error('SIGNUP CRASH DETECTED:', error);

    if (error.code === '23505') {
      return res.status(400).json({ error: 'Email or phone already exists' });
    }

    // Check for missing columns or schema errors
    if (error.message.includes('column "role" does not exist')) {
        console.error('SCHEMA ERROR: "role" column missing in users table.');
        return res.status(500).json({ error: 'Database out of sync. Please run migrate:up on VPS.' });
    }

    console.error('FULL SIGNUP ERROR STACK:', error);
    res.status(500).json({ error: 'Internal Server Error: ' + error.message });
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
      return res.status(400).json({ error: 'Invalid or expired OTP' });
    }

    const userId = rows[0].user_id;

    await db.query(
      "UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE id = $1",
      [userId]
    );

    // Delete used OTP
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [userId]);

    // Send Welcome Email (Background)
    notificationService.sendWelcomeEmail(userId).catch(e => console.error('Welcome email failed:', e.message));

    res.status(200).json({ message: 'Email verified successfully' });
  } catch (error) {
    console.error('Verification Error:', error);
    res.status(500).json({ error: 'Verification failed' });
  }
};

/**
 * Authenticates user and returns tokens.
 */
const login = async (req, res) => {
  const { email, password } = req.body;

  try {
    const { rows } = await db.query("SELECT * FROM users WHERE email = $1", [email]);
    if (rows.length === 0) return res.status(401).json({ error: 'Invalid credentials' });

    const user = rows[0];
    const isMatch = await authService.comparePassword(password, user.password_hash);
    if (!isMatch) return res.status(401).json({ error: 'Invalid credentials' });

    const tokens = authService.generateTokens(user);

    res.status(200).json({
      message: 'Login successful',
      userId: user.id,
      email: user.email,
      role: user.role,
      ...tokens
    });
  } catch (error) {
    console.error('Login Error:', error);
    res.status(500).json({ error: 'Login failed' });
  }
};

/**
 * Refreshes the access token using a refresh token.
 */
const refreshToken = (req, res) => {
    const { refreshToken } = req.body;
    if (!refreshToken) return res.status(401).json({ error: 'Refresh token required' });

    jwt.verify(refreshToken, authService.REFRESH_TOKEN_SECRET, (err, user) => {
      if (err) return res.status(403).json({ error: 'Invalid refresh token' });

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
    res.status(200).json({ message: 'Token updated' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to update token' });
  }
};

module.exports = {
  signup,
  verifyEmail,
  login,
  refreshToken,
  updateFCMToken
};
