const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const BREVO_USER = process.env.SMTP_USER;
const BREVO_PASS = process.env.SMTP_PASS;
const EMAIL_FROM = process.env.EMAIL_FROM || 'awagroceries@gmail.com';

// Initialize Brevo Transporter
let brevoTransporter;
if (BREVO_USER && BREVO_PASS) {
    brevoTransporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST || 'smtp-relay.brevo.com',
        port: parseInt(process.env.SMTP_PORT || '587'),
        secure: false,
        auth: {
            user: BREVO_USER,
            pass: BREVO_PASS,
        },
        logger: true,
        debug: true
    });
    console.log('✅ Brevo SMTP Email Service configured.');
} else {
    console.warn('⚠️ Brevo SMTP configuration missing. Emails will be logged to console only.');
}

/**
 * Sends an email using Brevo SMTP with Automatic Fallback to Console.
 * @param {string} to - Recipient email address.
 * @param {string} subject - Email subject.
 * @param {string} html - HTML content of the email.
 */
const sendMail = async (to, subject, html) => {
    const cleanFrom = EMAIL_FROM.replace(/["'<>]/g, '').trim();

    if (brevoTransporter) {
        try {
            console.log(`[Email] Attempting Brevo SMTP delivery to ${to} from ${cleanFrom}...`);
            const info = await brevoTransporter.sendMail({
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
            console.log(`[Brevo] SUCCESS: Message delivered. ID: ${info.messageId}`);
            return { success: true, messageId: info.messageId };
        } catch (error) {
            console.error(`[Brevo] FAILURE: Failed to send to ${to}`);
            console.error(`[Brevo] Error:`, error.message);
            // Proceed to console fallback on failure
        }
    }

    // Final Fallback: Console (Ensures system doesn't block during testing/errors)
    console.log('--- CRITICAL EMAIL FALLBACK (CONSOLE ONLY) ---');
    console.log(`To: ${to}`);
    console.log(`Subject: ${subject}`);
    console.log(`Body Snippet: ${html.substring(0, 150)}...`);
    console.log('----------------------------------------------');
    return { success: true, messageId: 'console-fallback' };
};

module.exports = { sendMail };
