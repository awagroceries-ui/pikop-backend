const db = require('../config/db');
const authService = require('../services/authService');
const emailService = require('../services/emailService');
const smsService = require('../services/smsService');
const { normalizePhone } = require('../utils/phone');
const crypto = require('crypto');

/**
 * Handles user registration.
 */
const signup = async (req, res) => {
  const { full_name, email, phone, password, role, referral_code } = req.body;
  const userRole = (role || 'CUSTOMER').toUpperCase();
  const normalizedPhone = normalizePhone(phone);

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
      [full_name, email, normalizedPhone, passwordHash, userRole, userReferralCode]
    );
    const user = userRes.rows[0];

    // 4. Generate OTP (Internal/Email)
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000); // 10 mins

    await client.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    // 5. Trigger Termii SMS OTP (External)
    const smsOtpRes = await smsService.sendOtp(normalizedPhone);
    if (smsOtpRes.success) {
        await client.query("UPDATE users SET kyc_provider_ref = $1 WHERE id = $2", [smsOtpRes.pinId, user.id]);
    }

    await client.query('COMMIT');

    // 6. Send OTP Email (Async)
    const subject = `Verify your Pikop Account: ${otp}`;
    const html = `
        <h2 class="greeting">Welcome to Pikop!</h2>
        <p class="text">We're excited to have you on board. To complete your registration and secure your account, please use the following verification code:</p>
        <div class="cta-container">
            <span class="otp-code">${otp}</span>
        </div>
        <p style="text-align: center;">We've also sent a verification code to your phone ${phone}.</p>
        <p class="text" style="text-align: center;">This code will expire in 10 minutes. If you did not request this, please ignore this email.</p>
    `;
    emailService.sendMail(email, subject, html).catch(err => console.error('[Auth] Initial OTP fail:', err.message));

    res.status(201).json({
      success: true,
      message: 'User registered. Please verify your email or phone.',
      userId: user.id,
      email: user.email,
      role: user.role
    });

  } catch (error) {
    await client.query('ROLLBACK');
    if (error.code === '23505') {
      return res.status(400).json({ success: false, message: 'Email or phone already registered' });
    }
    throw error;
  } finally {
    client.release();
  }
};

/**
 * Unified OTP Verification (Email/Internal or SMS/Termii).
 */
const verifyOtp = async (req, res) => {
  const { email, otp } = req.body;
  const masterOtp = process.env.MASTER_OTP;

  try {
    const { rows: users } = await db.query("SELECT * FROM users WHERE email = $1", [email]);
    if (users.length === 0) return res.status(404).json({ success: false, message: 'Account not found' });
    const user = users[0];

    let verified = false;

    // 1. Check Master OTP Bypass
    if (masterOtp && otp.toString().trim() === masterOtp.toString().trim()) {
        verified = true;
    }

    // 2. Check Internal Email OTP
    if (!verified) {
        const { rows: internalOtp } = await db.query(
            "SELECT * FROM otp_verifications WHERE user_id = $1 AND otp_code = $2 AND expires_at > CURRENT_TIMESTAMP",
            [user.id, otp]
        );
        if (internalOtp.length > 0) verified = true;
    }

    // 3. Check Termii SMS OTP
    if (!verified && user.kyc_provider_ref) {
        verified = await smsService.verifyOtpToken(user.kyc_provider_ref, otp);
    }

    if (!verified) {
        return res.status(400).json({ success: false, message: 'Invalid or expired verification code' });
    }

    // Mark as verified
    await db.query("UPDATE users SET email_verified_at = CURRENT_TIMESTAMP, phone_verified_at = CURRENT_TIMESTAMP WHERE id = $1", [user.id]);
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [user.id]);

    emailService.sendWelcomeEmail(user.email, user.full_name, user.role).catch(() => {});
    const tokens = authService.generateTokens(user);

    await db.query(
        "INSERT INTO user_sessions (user_id, refresh_token, ip_address) VALUES ($1, $2, $3)",
        [user.id, tokens.refreshToken, req.ip]
    );

    res.status(200).json({
        success: true,
        message: 'Account verified successfully',
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
 * Resends OTP with cooldown protection.
 */
const resendOtp = async (req, res) => {
  const { email } = req.body;

  try {
    const userRes = await db.query("SELECT * FROM users WHERE email = $1", [email]);
    if (userRes.rows.length === 0) return res.status(404).json({ success: false, message: 'Account not found' });
    const user = userRes.rows[0];

    // COOLDOWN: 60 seconds
    const lastOtp = await db.query("SELECT created_at FROM otp_verifications WHERE user_id = $1 ORDER BY created_at DESC LIMIT 1", [user.id]);
    if (lastOtp.rows.length > 0) {
        const timeSinceLast = Date.now() - new Date(lastOtp.rows[0].created_at).getTime();
        if (timeSinceLast < 60000) {
            return res.status(429).json({ success: false, message: `Please wait ${Math.ceil((60000 - timeSinceLast)/1000)}s before requesting another code.` });
        }
    }

    // 1. Internal Email OTP
    await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [user.id]);
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    await db.query("INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)", [user.id, otp, new Date(Date.now() + 10 * 60000)]);
    emailService.sendMail(email, `New Code: ${otp}`, `<p>Your code is <b>${otp}</b></p>`).catch(() => {});

    // 2. Termii SMS OTP
    const smsOtpRes = await smsService.sendOtp(user.phone);
    if (smsOtpRes.success) {
        await db.query("UPDATE users SET kyc_provider_ref = $1 WHERE id = $2", [smsOtpRes.pinId, user.id]);
    }

    res.status(200).json({ success: true, message: 'Verification code resent via email and SMS.' });
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
            "SELECT u.* FROM user_sessions s JOIN users u ON u.id = s.user_id WHERE s.refresh_token = $1 AND s.is_revoked = false",
            [refreshToken]
        );

        if (rows.length === 0) {
            return res.status(401).json({ success: false, message: 'Invalid or revoked refresh token' });
        }

        const user = rows[0];
        const tokens = authService.generateTokens(user);

        // Update session with new refresh token (Rotate)
        await db.query(
            "UPDATE user_sessions SET refresh_token = $1, last_active_at = CURRENT_TIMESTAMP WHERE refresh_token = $2",
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

/**
 * Updates the user's password.
 */
const changePassword = async (req, res) => {
    const { old_password, new_password } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query("SELECT * FROM users WHERE id = $1", [userId]);
        const user = rows[0];

        const isMatch = await authService.comparePassword(old_password, user.password_hash);
        if (!isMatch) return res.status(400).json({ success: false, message: 'Incorrect current password' });

        const newHash = await authService.hashPassword(new_password);
        await db.query("UPDATE users SET password_hash = $1 WHERE id = $2", [newHash, userId]);

        res.status(200).json({ success: true, message: 'Password updated successfully' });
    } catch (error) {
        throw error;
    }
};

/**
 * Performs a soft-delete of the user account.
 */
const deleteAccount = async (req, res) => {
    const userId = req.user.id;

    try {
        const anonymizedEmail = `deleted_${userId}@pikop.ng`;
        const anonymizedPhone = `deleted_${userId}`;

        await db.query(
            "UPDATE users SET status = 'deleted', email = $1, phone = $2, email_verified_at = NULL WHERE id = $3",
            [anonymizedEmail, anonymizedPhone, userId]
        );

        await db.query("UPDATE user_sessions SET is_revoked = true WHERE user_id = $1", [userId]);

        res.status(200).json({ success: true, message: 'Account deleted successfully' });
    } catch (error) {
        throw error;
    }
};

// V3 CONSOLIDATED EXPORT
module.exports = {
  signup,
  verifyOtp,
  login,
  resendOtp,
  refresh,
  updateFCMToken,
  changePassword,
  deleteAccount
};
