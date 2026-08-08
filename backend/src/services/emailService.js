const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp-relay.brevo.com',
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: false, // true for 465, false for other ports
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
  logger: true, // Enable logging for debugging
  debug: true
});

/**
 * Sends an email using the configured transporter.
 * @param {string} to - Recipient email.
 * @param {string} subject - Email subject.
 * @param {string} html - HTML content.
 */
const sendMail = async (to, subject, html) => {
  try {
    if (!process.env.SMTP_PASS) {
        console.error('Email Error: SMTP_PASS is missing in environment.');
        return { success: false, error: 'SMTP Configuration Missing' };
    }

    const info = await transporter.sendMail({
      from: process.env.EMAIL_FROM || '"Pikop by Awa" <awagroceries@gmail.com>',
      to,
      subject,
      html,
    });
    console.log('Email sent: %s', info.messageId);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error('Email Send Error:', error.message);
    return { success: false, error: error.message };
  }
};

module.exports = {
  sendMail
};
