const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

let transporter;

try {
    if (process.env.SMTP_HOST && process.env.SMTP_PASS) {
        transporter = nodemailer.createTransport({
            host: process.env.SMTP_HOST || 'smtp-relay.brevo.com',
            port: parseInt(process.env.SMTP_PORT || '587'),
            secure: false,
            auth: {
                user: process.env.SMTP_USER,
                pass: process.env.SMTP_PASS,
            },
            logger: true, // Enable built-in nodemailer logging
            debug: true   // Show SMTP traffic in console
        });
        console.log('✅ Email Transporter configured.');
    } else {
        console.warn('⚠️ SMTP Configuration missing. Emails will be logged to console only.');
    }
} catch (error) {
    console.error('❌ Email Config Error:', error.message);
}

/**
 * Sends an email using the configured transporter.
 */
const sendMail = async (to, subject, html) => {
  if (!transporter) {
    console.log('--- MOCK EMAIL ---');
    console.log('To:', to);
    console.log('Subject:', subject);
    // ...
    return { success: true, messageId: 'mock-id' };
  }

  try {
    const fromAddress = process.env.EMAIL_FROM || 'awagroceries@gmail.com';
    const info = await transporter.sendMail({
      from: fromAddress,
      to,
      subject,
      html,
    });
    console.log(`[SMTP] Success: Email sent to ${to}. ID: ${info.messageId}`);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error(`[SMTP] Failure: Failed to send to ${to}`);
    console.error(`[SMTP] Details:`, {
        code: error.code,
        response: error.response,
        responseCode: error.responseCode,
        command: error.command
    });

    if (error.responseCode === 535) {
        console.error('👉 TIP: Authentication failed. Verify SMTP_USER and SMTP_PASS (API Key) in Brevo.');
    } else if (error.code === 'EENVELOPE') {
        console.error('👉 TIP: Sender rejected. Ensure the EMAIL_FROM is verified in Brevo.');
    }

    return { success: false, error: error.message };
  }
};

module.exports = {
  sendMail
};
