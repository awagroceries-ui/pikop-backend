const nodemailer = require('nodemailer');

const SMTP_HOST = process.env.SMTP_HOST || 'smtp-relay.brevo.com';
const SMTP_PORT = parseInt(process.env.SMTP_PORT || '587');
const SMTP_USER = process.env.SMTP_USER;
const SMTP_PASS = process.env.SMTP_PASS;
const EMAIL_FROM = process.env.EMAIL_FROM || 'awagroceries@gmail.com';

let transporter;

if (SMTP_USER && SMTP_PASS) {
    transporter = nodemailer.createTransport({
        host: SMTP_HOST,
        port: SMTP_PORT,
        secure: false, // true for 465, false for other ports
        auth: {
            user: SMTP_USER,
            pass: SMTP_PASS,
        },
    });
    console.log('✅ Email Service: Brevo SMTP configured.');
} else {
    console.warn('⚠️ Email Service: Credentials missing. Emails will log to console only.');
}

/**
 * Sends a transactional email with high priority headers.
 */
const sendMail = async (to, subject, html) => {
  const cleanFrom = EMAIL_FROM.replace(/["'<>]/g, '').trim();

  if (!transporter) {
    console.log('\n--- MOCK EMAIL ---');
    console.log(`To: ${to}`);
    console.log(`Subject: ${subject}`);
    console.log('------------------\n');
    return { success: true, messageId: 'mock' };
  }

  try {
    const info = await transporter.sendMail({
      from: `"Pikop Support" <${cleanFrom}>`,
      to,
      subject,
      html,
      headers: {
        'X-Priority': '1 (Highest)',
        'X-MSMail-Priority': 'High',
        'Importance': 'High'
      }
    });

    console.log(`[Email] Sent to ${to}. ID: ${info.messageId}`);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error(`[Email] Failed to send to ${to}:`, error.message);
    return { success: false, error: error.message };
  }
};

module.exports = {
  sendMail
};
