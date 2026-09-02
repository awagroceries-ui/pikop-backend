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
      from: `"Pikop" <${cleanFrom}>`,
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

/**
 * Sends Welcome & Onboarding Email upon registration/verification.
 */
const sendWelcomeEmail = async (to, name, role) => {
    const isFulfiller = role === 'FULFILLER';
    const title = isFulfiller ? 'Welcome to the Pikop Fleet!' : 'Welcome to Pikop Logistics!';
    const html = `
        <h1 class="greeting">${title}</h1>
        <p class="text">Hello <strong>${name || 'Valued User'}</strong>,</p>
        <p class="text">Thank you for joining Pikop! Your account email has been successfully verified.</p>
        ${isFulfiller ? `
            <p class="text">As a Pikop Fulfiller, you are part of our elite delivery network in Nigeria. Please complete your identity and vehicle verification in the app to start accepting mission offers.</p>
            <div style="background: #F0FDF4; padding: 20px; border-radius: 16px; border-left: 4px solid #008751; margin-bottom: 25px;">
                <strong style="color: #008751;">Next Steps:</strong>
                <ol style="margin-top: 10px; padding-left: 20px; color: #374151;">
                    <li>Open Pikop App -> Go to Account Activation</li>
                    <li>Capture your live profile photo</li>
                    <li>Complete Identity Check & Vehicle Details</li>
                    <li>Submit for Admin Approval & Go Online!</li>
                </ol>
            </div>
        ` : `
            <p class="text">You can now send packages, track deliveries in real-time, and manage your logistics effortlessly across Lagos and beyond.</p>
        `}
        <p class="text">If you ever need assistance, our support team is available 24/7 in the app Help Center.</p>
    `;
    return await sendMail(to, title, html);
};

/**
 * Sends KYC Approval or Rejection Email to Fulfillers.
 */
const sendKycStatusEmail = async (to, name, status, note) => {
    const isApproved = status === 'VERIFIED';
    const subject = isApproved ? 'Your Pikop Fulfiller Account is Approved!' : 'Pikop Verification Status Update';

    const html = isApproved ? `
        <h1 class="greeting" style="color: #008751;">Congratulations! You are Approved! 🎉</h1>
        <p class="text">Hello <strong>${name || 'Agent'}</strong>,</p>
        <p class="text">Your KYC verification and document review have been successfully approved by our administration team. Your account is now fully active.</p>

        <div style="background: #F0FDF4; padding: 24px; border-radius: 20px; border: 1px solid #BBF7D0; margin: 30px 0;">
            <h3 style="margin-top: 0; color: #008751;">🚀 Fulfiller Operating Guidelines:</h3>
            <ul style="color: #374151; line-height: 1.8; margin-bottom: 0;">
                <li><strong>Go Online:</strong> Toggle your status switch on the dashboard when ready to receive nearby delivery offers.</li>
                <li><strong>Pickup Protocol:</strong> Verify package details and confirm code with the sender before moving.</li>
                <li><strong>Safe Transport:</strong> Keep items secure and deliver within estimated windows.</li>
                <li><strong>Delivery Verification:</strong> Confirm delivery code with recipient to complete mission & unlock earnings.</li>
            </ul>
        </div>

        <p class="text">Open your Pikop app now, go online, and start earning!</p>
    ` : `
        <h1 class="greeting" style="color: #DC2626;">Verification Action Required</h1>
        <p class="text">Hello <strong>${name || 'Applicant'}</strong>,</p>
        <p class="text">We reviewed your submitted verification documents for your Pikop Fulfiller application.</p>
        <p class="text"><strong>Reason / Note:</strong> ${note || 'Document details were unclear or unverified.'}</p>
        <p class="text">Please log back into the Pikop app, re-upload clear photos of your documents or vehicle details, and resubmit for approval.</p>
    `;

    return await sendMail(to, subject, html);
};

/**
 * Sends Payment Confirmation / Receipt Email.
 */
const sendPaymentReceiptEmail = async (to, name, orderId, amount, itemDesc) => {
    const subject = `Payment Confirmation - Mission #${orderId}`;
    const html = `
        <h1 class="greeting">Payment Received!</h1>
        <p class="text">Hello <strong>${name || 'Customer'}</strong>,</p>
        <p class="text">We've confirmed your payment for delivery mission <strong>#${orderId}</strong>.</p>

        <div style="background: #F9FAFB; padding: 24px; border-radius: 20px; border: 1px solid #E5E7EB; margin: 30px 0;">
            <table style="width: 100%; border-collapse: collapse;">
                <tr><td style="padding: 8px 0; color: #6B7280;">Mission ID:</td><td style="text-align: right; font-weight: 700; color: #111827;">#${orderId}</td></tr>
                <tr><td style="padding: 8px 0; color: #6B7280;">Item Description:</td><td style="text-align: right; font-weight: 600; color: #111827;">${itemDesc || 'Package'}</td></tr>
                <tr><td style="padding: 8px 0; color: #6B7280; font-weight: 700;">Total Paid:</td><td style="text-align: right; font-weight: 800; color: #008751; font-size: 18px;">₦${parseFloat(amount || 0).toLocaleString()}</td></tr>
            </table>
        </div>

        <p class="text">Our dispatch system is searching for the nearest available fulfiller. You can track your mission live in the Pikop app!</p>
    `;

    return await sendMail(to, subject, html);
};

/**
 * Sends Order Completion Notice Email.
 */
const sendOrderCompletionEmail = async (to, name, orderId, totalFare) => {
    const subject = `Mission #${orderId} Delivered!`;
    const html = `
        <h1 class="greeting" style="color: #008751;">Mission Completed!</h1>
        <p class="text">Hello <strong>${name || 'Customer'}</strong>,</p>
        <p class="text">Your delivery mission <strong>#${orderId}</strong> has been successfully completed and delivered.</p>

        <p class="text">Thank you for choosing Pikop Logistics! Please rate your agent's service in the app to help us maintain top quality.</p>
    `;

    return await sendMail(to, subject, html);
};

module.exports = {
  sendMail,
  sendWelcomeEmail,
  sendKycStatusEmail,
  sendPaymentReceiptEmail,
  sendOrderCompletionEmail
};
