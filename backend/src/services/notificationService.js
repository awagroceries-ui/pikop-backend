const db = require('../config/db');
const emailService = require('./emailService');

/**
 * Logs a notification attempt to the database.
 */
const logNotification = async (userId, channel, templateName, recipient, status, error = null) => {
  try {
    await db.query(
      `INSERT INTO notification_logs (user_id, channel, template_name, recipient, status, error_message)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, channel, templateName, recipient, status, error]
    );
  } catch (e) {
    console.error('Failed to log notification:', e.message);
  }
};

/**
 * Checks if a specific notification has already been sent to a user.
 */
const hasAlreadySent = async (userId, templateName) => {
  const { rows } = await db.query(
    "SELECT id FROM notification_logs WHERE user_id = $1 AND template_name = $2 AND status = 'SUCCESS'",
    [userId, templateName]
  );
  return rows.length > 0;
};

/**
 * Sends OTP Email for signup verification.
 */
const sendOTPEmail = async (userId, email, otp) => {
  const subject = `Your Pikop Verification Code: ${otp}`;
  const logoUrl = 'https://api.awa.name.ng/public/assets/logo.png';

  const html = `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 0; border-radius: 12px; overflow: hidden; background-color: #0B0B0B; color: #FFFFFF; border: 1px solid #334155;">
      <div style="background-color: #1E293B; padding: 30px; text-align: center;">
        <img src="${logoUrl}" alt="Pikop Logo" style="width: 120px; height: auto;">
      </div>
      <div style="padding: 40px; text-align: center;">
        <h2 style="color: #FF9F0A; margin-top: 0;">Verify your email</h2>
        <p style="font-size: 16px; line-height: 1.5;">Welcome to Pikop! Use the code below to complete your registration and join the fleet.</p>
        <div style="background: #1E293B; border: 2px dashed #FF9F0A; border-radius: 8px; margin: 30px 0; padding: 20px; font-size: 36px; font-weight: bold; letter-spacing: 10px; color: #FF9F0A;">
          ${otp}
        </div>
        <p style="color: #94A3B8; font-size: 14px;">This code will expire in 10 minutes. If you didn't request this, you can safely ignore this email.</p>
        <hr style="border: none; border-top: 1px solid #334155; margin: 30px 0;">
        <p style="font-size: 12px; color: #64748B;">© 2026 Awa Foods & Groceries, Nigeria.</p>
      </div>
    </div>
  `;

  const result = await emailService.sendMail(email, subject, html);
  await logNotification(userId, 'email', 'otp_verification', email, result.success ? 'SUCCESS' : 'FAILED', result.error);
};

/**
 * Sends a warm welcome email to newly verified users.
 */
const sendWelcomeEmail = async (userId) => {
  if (await hasAlreadySent(userId, 'welcome_user')) return;

  try {
    const { rows } = await db.query("SELECT email, full_name FROM users WHERE id = $1", [userId]);
    if (rows.length === 0) return;
    const user = rows[0];

    const subject = "Welcome to Pikop - Your account is active!";
    const logoUrl = 'https://api.awa.name.ng/public/assets/logo.png';

    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 0; border-radius: 12px; overflow: hidden; background-color: #0B0B0B; color: #FFFFFF; border: 1px solid #334155;">
        <div style="background-color: #1E293B; padding: 30px; text-align: center;">
          <img src="${logoUrl}" alt="Pikop Logo" style="width: 120px; height: auto;">
        </div>
        <div style="padding: 40px;">
          <h1 style="color: #FF9F0A; margin-top: 0;">Welcome, ${user.full_name.split(' ')[0]}!</h1>
          <p style="font-size: 16px; line-height: 1.6;">Your account is now active and verified. Pikop makes moving anything across the city as simple as a tap.</p>

          <div style="margin: 40px 0; text-align: center;">
            <a href="https://api.awa.name.ng" style="background-color: #FF9F0A; color: #0B0B0B; padding: 18px 40px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">Request your first delivery</a>
          </div>

          <p style="color: #94A3B8; font-size: 15px;">Whether it's a small package or a large delivery, we've got you covered across Lagos, Abuja, and Port Harcourt.</p>

          <p style="margin-top: 40px; color: #94A3B8; font-size: 14px;">Happy delivering!<br>The Pikop Team</p>
          <hr style="border: none; border-top: 1px solid #334155; margin: 30px 0;">
          <p style="font-size: 12px; color: #64748B; text-align: center;">© 2026 Awa Foods & Groceries, Nigeria.</p>
        </div>
      </div>
    `;

    const result = await emailService.sendMail(user.email, subject, html);
    await logNotification(userId, 'email', 'welcome_user', user.email, result.success ? 'SUCCESS' : 'FAILED', result.error);
  } catch (error) {
    console.error('Welcome Email Error:', error.message);
  }
};

/**
 * Confirms fulfiller approval and provides next steps.
 */
const sendFulfillerApprovedEmail = async (fulfillerId) => {
  try {
    const { rows } = await db.query(`
      SELECT u.id as user_id, u.email, u.full_name, f.kyc_status
      FROM fulfillers f
      JOIN users u ON u.id = f.user_id
      WHERE f.id = $1
    `, [fulfillerId]);

    if (rows.length === 0) return;
    const user = rows[0];

    if (await hasAlreadySent(user.user_id, 'fulfiller_approved')) return;

    const subject = "Congratulations! Your Pikop Fulfiller application is approved";
    const logoUrl = 'https://api.awa.name.ng/public/assets/logo.png';

    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 0; border-radius: 12px; overflow: hidden; background-color: #0B0B0B; color: #FFFFFF; border: 1px solid #334155;">
        <div style="background-color: #1E293B; padding: 30px; text-align: center;">
          <img src="${logoUrl}" alt="Pikop Logo" style="width: 120px; height: auto;">
        </div>
        <div style="padding: 40px;">
          <h2 style="color: #FF9F0A; margin-top: 0;">You're ready to start earning!</h2>
          <p style="font-size: 16px; line-height: 1.6;">Hello ${user.full_name.split(' ')[0]}, your application to become a Pikop Fulfiller has been <strong>approved</strong>.</p>

          <div style="background: rgba(255, 159, 10, 0.1); padding: 20px; border-left: 4px solid #FF9F0A; margin: 30px 0; border-radius: 4px;">
            <strong style="color: #FF9F0A; display: block; margin-bottom: 8px;">Next Action:</strong>
            Open the app and toggle <strong>"Online"</strong> to start receiving delivery requests in your area.
          </div>

          <h3 style="color: #38BDF8;">Earnings & Payouts</h3>
          <ul style="color: #CBD5E1; line-height: 1.8; font-size: 15px;">
            <li><strong>75/25 Split:</strong> You keep 75% of every delivery fare.</li>
            <li><strong>Instant Payouts:</strong> Request your earnings anytime via the Wallet in the app.</li>
            <li><strong>Nationwide Service:</strong> Now active in Lagos, Abuja, and Port Harcourt.</li>
          </ul>

          <p style="margin-top: 40px;">Drive safe and welcome to the fleet!</p>
          <p style="color: #94A3B8;">The Pikop Operations Team</p>
          <hr style="border: none; border-top: 1px solid #334155; margin: 30px 0;">
          <p style="font-size: 12px; color: #64748B; text-align: center;">A product of Awa Foods & Groceries, Nigeria.</p>
        </div>
      </div>
    `;

    const result = await emailService.sendMail(user.email, subject, html);
    await logNotification(user.user_id, 'email', 'fulfiller_approved', user.email, result.success ? 'SUCCESS' : 'FAILED', result.error);
  } catch (error) {
    console.error('Fulfiller Approved Email Error:', error.message);
  }
};

module.exports = {
  sendOTPEmail,
  sendWelcomeEmail,
  sendFulfillerApprovedEmail
};
