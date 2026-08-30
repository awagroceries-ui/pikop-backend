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
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap');
            body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; background-color: #F3F4F6; margin: 0; padding: 0; -webkit-font-smoothing: antialiased; }
            .wrapper { width: 100%; padding: 40px 0; background-color: #F3F4F6; }
            .container { width: 100%; max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 32px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.08); }
            .header { background-color: #008751; padding: 50px 40px; text-align: center; }
            .logo { height: 42px; width: auto; filter: brightness(0) invert(1); }
            .content { padding: 50px 40px; color: #1F2937; }
            .greeting { font-size: 24px; font-weight: 800; color: #111827; margin-bottom: 20px; letter-spacing: -0.025em; }
            .text { font-size: 16px; line-height: 1.6; color: #4B5563; margin-bottom: 30px; }
            .cta-container { margin: 40px 0; text-align: center; }
            .otp-code { font-size: 42px; font-weight: 900; letter-spacing: 12px; color: #008751; background: #F0FDF4; padding: 24px; border-radius: 20px; display: inline-block; border: 2px dashed #008751; }
            .footer { padding: 40px; text-align: center; background: #F9FAFB; border-top: 1px solid #F3F4F6; }
            .footer-text { font-size: 13px; color: #9CA3AF; line-height: 1.6; }
            .social-links { margin-top: 20px; }
            .social-link { color: #008751; text-decoration: none; font-weight: 600; margin: 0 10px; font-size: 13px; }
            @media (max-width: 600px) {
                .container { border-radius: 0; margin: 0; }
                .content { padding: 40px 24px; }
                .header { padding: 40px 24px; }
            }
        </style>
    </head>
    <body>
        <div class="wrapper">
            <div class="container">
                <div class="header">
                    <img src="https://api.pikop.com.ng/public/assets/logo.png" alt="Pikop Logo" class="logo">
                </div>
                <div class="content">
                    ${html}
                </div>
                <div class="footer">
                    <p class="footer-text">
                        <strong>&copy; 2026 Pikop Logistics Limited.</strong><br>
                        Superior Multi-Platform Logistics Engine.<br>
                        Lagos, Nigeria.
                    </p>
                    <div class="social-links">
                        <a href="https://pikop.com.ng" class="social-link">Website</a>
                        <a href="https://api.pikop.com.ng/support" class="social-link">Support</a>
                    </div>
                </div>
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

    console.log(`[Email] Branded success! Delivered to ${to}.`);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error(`[Email] CRITICAL FAILURE for ${to}:`, {
        message: error.message,
        code: error.code,
        command: error.command,
        response: error.response
    });
    return { success: false, error: error.message };
  }
};

module.exports = { sendMail };
