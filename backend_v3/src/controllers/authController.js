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
  const {
      full_name, email, phone, password, role, referral_code,
      primary_class, date_of_birth, home_address, gender,
      registration_number, make, model, color,
      terms_version, privacy_version,
      fleet_invite_code
  } = req.body;
  const userRole = (role || 'CUSTOMER').toUpperCase();
  const normalizedPhone = normalizePhone(phone);
  const emailTrimmed = (email || '').trim().toLowerCase();

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailTrimmed || !emailRegex.test(emailTrimmed)) {
      return res.status(400).json({
          success: false,
          message: 'Please enter a valid email address (e.g. user@example.com). Physical addresses are not permitted as usernames.'
      });
  }

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

    // 4. If Fulfiller, create initial fulfiller profile
    if (userRole === 'FULFILLER') {
        let fleetPartnerId = null;
        if (fleet_invite_code) {
            const fleetRes = await client.query("SELECT fleet_partner_id FROM fleet_partner_invites WHERE invite_code = $1 AND is_active = true", [fleet_invite_code]);
            if (fleetRes.rows.length > 0) fleetPartnerId = fleetRes.rows[0].fleet_partner_id;
        }

        await client.query(
            `INSERT INTO fulfillers (
                user_id, full_name, email, phone, password_hash, primary_class,
                date_of_birth, home_address, gender,
                registration_number, make, model, color,
                fleet_partner_id
             ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)`,
            [
                user.id, full_name, email, normalizedPhone, passwordHash, (primary_class || 'rider').toLowerCase(),
                date_of_birth, home_address, gender,
                registration_number, make, model, color,
                fleetPartnerId
            ]
        );
    }

    // 6. Generate OTP (Internal/Email)
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60000); // 10 mins

    await client.query(
      "INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)",
      [user.id, otp, expiresAt]
    );

    // 5. Trigger Termii SMS OTP (External)
    const smsOtpRes = await smsService.sendOtp(normalizedPhone);
    if (smsOtpRes.success) {
        console.log(`[Auth] SMS OTP successfully triggered for user_id: ${user.id}`);
        await client.query("UPDATE users SET kyc_provider_ref = $1 WHERE id = $2", [smsOtpRes.pinId, user.id]);
    } else {
        console.warn(`[Auth] SMS OTP failed to trigger for ${user.email}. Error: ${smsOtpRes.error}`);
    }

    await client.query('COMMIT');

    // 7. Record Legal Consent (AFTER COMMIT - Resilient/Non-blocking)
    if (terms_version && privacy_version) {
        try {
            await db.query(
                `INSERT INTO user_legal_consents (user_id, terms_version, privacy_version, ip_address)
                 VALUES ($1, $2, $3, $4)`,
                [user.id, terms_version, privacy_version, req.ip]
            );
        } catch (consentErr) {
            console.warn(`[Auth] Legal consent recording skipped (likely table missing): ${consentErr.message}`);
        }
    }

    // 8. DEFERRED: Email OTP is now a fallback. Only Welcome email is sent after verification.
    console.log(`[Auth] Signup success for user_id: ${user.id}. SMS OTP triggered.`);

    res.status(201).json({
      success: true,
      message: 'User registered. Please verify your email or phone.',
      userId: user.id,
      email: user.email,
      role: user.role
    });

  } catch (error) {
    await client.query('ROLLBACK');
    console.error('[Auth] Signup FATAL Error:', error);

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

    // Send Welcome Email only for CUSTOMER immediately. Fulfillers/Merchants send on Approval.
    if (user.role === 'CUSTOMER') {
        emailService.sendWelcomeEmail(user.email, user.full_name, user.role).catch(() => {});
    }

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

        // 1. Refresh Internal OTP (in case user switches to email fallback after resend)
        await db.query("DELETE FROM otp_verifications WHERE user_id = $1", [user.id]);
        const otp = Math.floor(100000 + Math.random() * 900000).toString();
        await db.query("INSERT INTO otp_verifications (user_id, otp_code, expires_at) VALUES ($1, $2, $3)", [user.id, otp, new Date(Date.now() + 10 * 60000)]);

        // 2. Primarily trigger Termii SMS OTP
        const smsOtpRes = await smsService.sendOtp(user.phone);
        if (smsOtpRes.success) {
            await db.query("UPDATE users SET kyc_provider_ref = $1 WHERE id = $2", [smsOtpRes.pinId, user.id]);
        }

        res.status(200).json({ success: true, message: 'New verification code sent via SMS.' });
    } catch (error) {
        throw error;
    }
};

/**
 * Fallback: Sends the current valid OTP to the user's email if SMS was missed.
 */
const requestEmailOtp = async (req, res) => {
    const { email } = req.body;

    try {
        const { rows } = await db.query("SELECT id, full_name FROM users WHERE email = $1", [email]);
        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Account not found' });
        const user = rows[0];

        // Fetch the most recent valid OTP
        const otpRes = await db.query(
            "SELECT otp_code FROM otp_verifications WHERE user_id = $1 AND expires_at > CURRENT_TIMESTAMP ORDER BY created_at DESC LIMIT 1",
            [user.id]
        );

        if (otpRes.rows.length === 0) {
            return res.status(400).json({ success: false, message: 'No active verification code found. Please use Resend instead.' });
        }

        const otp = otpRes.rows[0].otp_code;

        // Send Email
        const subject = `Your Pikop Verification Code: ${otp}`;
        const html = `
            <h2 class="greeting">Email Verification Fallback</h2>
            <p class="text">You requested to receive your verification code via email. Please use the code below to activate your account:</p>
            <div class="cta-container" style="text-align: center; margin: 30px 0;">
                <span class="otp-code" style="font-size: 32px; font-weight: 800; color: #008751; letter-spacing: 5px; border: 2px dashed #008751; padding: 10px 20px; border-radius: 8px;">${otp}</span>
            </div>
            <p class="text">This code is valid for 10 minutes. If you did not request this, please secure your account.</p>
        `;

        await emailService.sendMail(email, subject, html);

        res.status(200).json({ success: true, message: 'Verification code sent to your email.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
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
            "INSERT INTO user_fcm_tokens (user_id, token, updated_at) VALUES ($1, $2, CURRENT_TIMESTAMP) ON CONFLICT (user_id) DO UPDATE SET token = $2, updated_at = CURRENT_TIMESTAMP",
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
/**
 * Verifies identity via password before sensitive operations.
 */
const confirmPassword = async (req, res) => {
    const { password } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query("SELECT password_hash FROM users WHERE id = $1", [userId]);
        if (rows.length === 0) return res.status(404).json({ success: false, message: 'User not found' });

        const isMatch = await authService.comparePassword(password, rows[0].password_hash);
        res.status(200).json({ success: isMatch, message: isMatch ? 'Identity confirmed' : 'Incorrect password' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Performs a hardened soft-delete of the user account.
 */
const deleteAccount = async (req, res) => {
    const userId = req.user.id;
    const client = await db.pool.connect();

    try {
        await client.query('BEGIN');

        // 1. GATING: Check for Active Missions
        const { rows: activeMissions } = await client.query(`
            SELECT id FROM orders
            WHERE user_id = $1
              AND status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED', 'RECIPIENT_ABSENT')
        `, [userId]);

        if (activeMissions.length > 0) {
            await client.query('ROLLBACK');
            return res.status(400).json({
                success: false,
                message: `You have ${activeMissions.length} active mission(s) in progress. Please complete them before deleting your account.`
            });
        }

        // 2. GATING: Check Wallet Balance (safely parse numbers and handle floating point noise)
        const { rows: wallet } = await client.query("SELECT balance, pending_balance FROM wallets WHERE owner_type = 'USER' AND owner_id = $1", [userId.toString()]);
        if (wallet.length > 0) {
            const bal = Math.abs(parseFloat(wallet[0].balance || 0));
            const pend = Math.abs(parseFloat(wallet[0].pending_balance || 0));
            if (bal >= 0.01 || pend >= 0.01) {
                await client.query('ROLLBACK');
                return res.status(400).json({
                    success: false,
                    message: `You have a non-zero wallet balance (₦${bal.toFixed(2)}). Please withdraw your funds before deleting your account.`
                });
            }
        }

        // 3. GATING: Check for Unresolved Disputes
        const { rows: disputes } = await client.query(`
            SELECT id FROM disputes
            WHERE reporter_id = $1 AND status IN ('OPEN', 'INVESTIGATING')
        `, [userId]);

        if (disputes.length > 0) {
            await client.query('ROLLBACK');
            return res.status(400).json({
                success: false,
                message: "You have open disputes under investigation. Please resolve them with support before deleting your account."
            });
        }

        // 4. ANONYMIZATION: Scramble PII in Users table with unique timestamp
        const timestamp = Date.now().toString().slice(-6);
        const anonymizedEmail = `deleted_${userId}_${timestamp}@pikop.ng`;
        const anonymizedPhone = `del_${userId}_${timestamp}`;

        await client.query(
            `UPDATE users
             SET full_name = 'Deleted User',
                 email = $1,
                 phone = $2,
                 password_hash = '*',
                 profile_photo_url = NULL,
                 status = 'deleted',
                 email_verified_at = NULL,
                 phone_verified_at = NULL,
                 kyc_provider_ref = NULL
             WHERE id = $3`,
            [anonymizedEmail, anonymizedPhone, userId]
        );

        // 5. CLEANUP: Delete linked documents and tokens safely
        await client.query("DELETE FROM kyc_documents WHERE fulfiller_id IN (SELECT id FROM fulfillers WHERE user_id = $1)", [userId]).catch(() => {});
        await client.query("DELETE FROM user_fcm_tokens WHERE user_id = $1", [userId]).catch(() => {});

        // 6. ROLE STATUS: Suspend linked business entities safely
        await client.query("UPDATE fulfillers SET status = 'deleted', online_status = 'OFFLINE' WHERE user_id = $1", [userId]).catch(() => {});
        await client.query("UPDATE vendors SET status = 'suspended' WHERE user_id = $1", [userId]).catch(() => {});
        await client.query("UPDATE kitchens SET status = 'suspended' WHERE user_id = $1", [userId]).catch(() => {});
        await client.query("UPDATE fleet_partners SET status = 'SUSPENDED' WHERE user_id = $1", [userId]).catch(() => {});

        // 7. SESSION: Revoke all access
        await client.query("UPDATE user_sessions SET is_revoked = true WHERE user_id = $1", [userId]).catch(() => {});

        await client.query('COMMIT');
        console.log(`[Account] User ${userId} successfully deleted and anonymized.`);

        res.status(200).json({ success: true, message: 'Your account and personal data have been removed.' });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Account Delete] FATAL Error:', error.stack || error.message);
        res.status(500).json({ success: false, message: error.message || 'An error occurred during account deletion.' });
    } finally {
        client.release();
    }
};

module.exports = {
  signup,
  verifyOtp,
  login,
  resendOtp,
  requestEmailOtp,
  refresh,
  updateFCMToken,
  changePassword,
  confirmPassword,
  deleteAccount
};
