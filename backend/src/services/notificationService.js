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
  const html = `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: #FF9F0A;">Verify your email</h2>
      <p>Welcome to Pikop! Use the code below to complete your registration:</p>
      <div style="background: #f4f4f4; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #0B0B0B;">
        ${otp}
      </div>
      <p style="color: #666; font-size: 12px; margin-top: 20px;">This code will expire in 10 minutes. If you didn't request this, you can safely ignore this email.</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 10px; color: #999;">Pikop is a product of Awa Foods & Groceries, Nigeria.</p>
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
    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px; background-color: #0B0B0B; color: #FFFFFF;">
        <h1 style="color: #FF9F0A;">Welcome, ${user.full_name.split(' ')[0]}!</h1>
        <p>Your account is now active and verified.</p>
        <p>Pikop makes moving anything across the city as simple as a tap. Whether it's a small package or a large delivery, we've got you covered.</p>
        <div style="margin: 30px 0; text-align: center;">
          <a href="https://pikop.ng" style="background-color: #FF9F0A; color: #0B0B0B; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Request your first delivery</a>
        </div>
        <p style="color: #94A3B8; font-size: 14px;">Happy delivering!<br>The Pikop Team</p>
        <hr style="border: none; border-top: 1px solid #334155; margin: 20px 0;">
        <p style="font-size: 10px; color: #94A3B8;">© 2026 Awa Foods & Groceries, Nigeria.</p>
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
    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
        <h2 style="color: #FF9F0A;">You're ready to start earning!</h2>
        <p>Hello ${user.full_name.split(' ')[0]}, your application to become a Pikop Fulfiller has been approved.</p>

        <div style="background: #f9f9f9; padding: 15px; border-left: 4px solid #FF9F0A; margin: 20px 0;">
          <strong>Next Action:</strong> Open the app and toggle <strong>"Online"</strong> to start receiving delivery requests in your area.
        </div>

        <h3>Earnings & Payouts</h3>
        <ul style="color: #444; line-height: 1.6;">
          <li><strong>75/25 Split:</strong> You keep 75% of every delivery fare.</li>
          <li><strong>Instant Payouts:</strong> Request your earnings anytime via the Wallet in the app.</li>
          <li><strong>Standard Payouts:</strong> Regular weekly distributions to your linked bank account.</li>
        </ul>

        <p>Drive safe and welcome to the fleet!</p>
        <p style="color: #666;">The Pikop Operations Team</p>
        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
        <p style="font-size: 10px; color: #999;">Pikop is a product of Awa Foods & Groceries, Nigeria.</p>
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
