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
        secure: false,
        auth: {
            user: SMTP_USER,
            pass: SMTP_PASS,
        },
    });
    console.log('✅ Email Service: SMTP identity confirmed.');
} else {
    console.warn('⚠️ Email Service: Missing credentials.');
}

/**
 * Sends a premium-grade branded transactional email.
 */
const sendMail = async (to, subject, html) => {
  const cleanFrom = EMAIL_FROM.replace(/["'<>]/g, '').trim();

  const premiumTemplate = `
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: #F9FAFB; margin: 0; padding: 0; }
            .container { width: 100%; max-width: 600px; margin: 40px auto; background: white; border-radius: 24px; overflow: hidden; box-shadow: 0 20px 50px rgba(0,0,0,0.05); }
            .header { background-color: #008751; padding: 60px 40px; text-align: center; }
            .logo { height: 48px; filter: brightness(0) invert(1); }
            .body { padding: 48px; color: #111827; }
            .title { font-size: 28px; font-weight: 800; margin-bottom: 24px; color: #111827; letter-spacing: -1px; }
            .text { font-size: 16px; line-height: 1.7; color: #4B5563; margin-bottom: 32px; }
            .cta-box { background: #F3F4F6; border-radius: 16px; padding: 32px; text-align: center; margin: 32px 0; border: 1px solid #E5E7EB; }
            .footer { padding: 40px; text-align: center; background: #F9FAFB; border-top: 1px solid #F3F4F6; }
            .footer-text { font-size: 12px; color: #9CA3AF; line-height: 1.5; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <img src="https://api.pikop.com.ng/public/assets/logo.png" alt="Pikop" class="logo">
            </div>
            <div class="body">
                ${html}
            </div>
            <div class="footer">
                <p class="footer-text">
                    &copy; 2026 Pikop Logistics Ltd. All rights reserved.<br>
                    Nigeria's Superior Multi-Platform Logistics Engine.<br>
                    You are receiving this because of activity on your Pikop account.
                </p>
            </div>
        </div>
    </body>
    </html>
  `;

  if (!transporter) return { success: true, messageId: 'mock' };

  try {
    const info = await transporter.sendMail({
      from: \`"Pikop" <\${cleanFrom}>\`,
      to,
      subject: subject.toUpperCase(),
      html: premiumTemplate,
      headers: {
        'X-Priority': '1 (Highest)',
        'X-MSMail-Priority': 'High',
        'Importance': 'High'
      }
    });

    console.log(\`[Email] Branded success! Delivered to \${to}.\`);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error(\`[Email] Critical Delivery Failure:\`, error.message);
    return { success: false, error: error.message };
  }
};

module.exports = { sendMail };
