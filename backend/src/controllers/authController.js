const db = require('../config/db');
const authService = require('../services/authService');
const jwt = require('jsonwebtoken');

/**
 * Registers a new user and generates an OTP.
 */
const signup = async (req, res) => {
  const { full_name, email, phone, password } = req.body;

  try {
    const passwordHash = await authService.hashPassword(password);

    const userRes = await db.query(
      "INSERT INTO users (full_name, email, phone, password_hash) VALUES ($1, $2, $3, $4) RETURNING id, email",
      [full_name, email, phone, passwordHash]
    );
    const user = userRes.rows[0];

    // Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000); // 10 minutes

    await db.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    // TODO: Send email with OTP using an email service
    console.log(`OTP for ${email}: ${otp}`);

    const tokens = authService.generateTokens(user);

    res.status(201).json({
      message: 'User registered. Please verify your email.',
      userId: user.id,
      email: user.email,
      ...tokens
    });
  } catch (error) {
    if (error.code === '23505') {
      return res.status(400).json({ error: 'Email or phone already exists' });
    }
    res.status(500).json({ error: 'Failed to register user' });
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

    await db.query(
      "UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE id = $1",
      [rows[0].user_id]
    );

    // Delete used OTP
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [rows[0].user_id]);

    res.status(200).json({ message: 'Email verified successfully' });
  } catch (error) {
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
      ...tokens
    });
  } catch (error) {
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

module.exports = {
  signup,
  verifyEmail,
  login,
  refreshToken
};
