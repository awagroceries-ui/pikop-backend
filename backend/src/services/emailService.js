const axios = require('axios');
const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const MAILJET_API_KEY = process.env.MAILJET_API_KEY;
const MAILJET_SECRET_KEY = process.env.MAILJET_SECRET_KEY;
const BREVO_USER = process.env.SMTP_USER; // Legacy Brevo/SIB Login
const BREVO_PASS = process.env.SMTP_PASS; // Legacy Brevo/SIB Key
const EMAIL_FROM = process.env.EMAIL_FROM || 'awagroceries@gmail.com';

// 1. Initialize Brevo Transporter as fallback
let brevoTransporter;
if (BREVO_USER && BREVO_PASS) {
    brevoTransporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST || 'smtp-relay.brevo.com',
        port: parseInt(process.env.SMTP_PORT || '587'),
        secure: false,
        auth: { user: BREVO_USER, pass: BREVO_PASS }
    });
}

/**
 * Universal Send Mail with Automatic Fallback.
 * Try Mailjet -> Try Brevo -> Fallback to Console.
 */
const sendMail = async (to, subject, html) => {
    const cleanFrom = EMAIL_FROM.replace(/["'<>]/g, '').trim();

    // Provider 1: Mailjet API
    if (MAILJET_API_KEY && MAILJET_SECRET_KEY) {
        try {
            console.log(`[Email] Attempting Mailjet delivery to ${to}...`);
            const response = await axios.post('https://api.mailjet.com/v3.1/send', {
                Messages: [{
                    From: { Email: cleanFrom, Name: "Pikop" },
                    To: [{ Email: to }],
                    Subject: subject,
                    HTMLPart: html,
                    TextPart: html.replace(/<[^>]*>?/gm, '')
                }]
            }, {
                auth: { username: MAILJET_API_KEY, password: MAILJET_SECRET_KEY },
                timeout: 5000
            });
            console.log(`[Mailjet] SUCCESS: ID ${response.data.Messages[0].To[0].MessageID}`);
            return { success: true, provider: 'mailjet' };
        } catch (error) {
            console.warn(`[Mailjet] FAILED: ${error.response?.data?.ErrorMessage || error.message}`);
            // If blocked or unauthorized, proceed to fallback
        }
    }

    // Provider 2: Brevo (SMTP)
    if (brevoTransporter) {
        try {
            console.log(`[Email] Falling back to Brevo SMTP for ${to}...`);
            const info = await brevoTransporter.sendMail({
                from: `"Pikop" <${cleanFrom}>`,
                to,
                subject,
                html
            });
            console.log(`[Brevo] SUCCESS: ID ${info.messageId}`);
            return { success: true, provider: 'brevo' };
        } catch (error) {
            console.warn(`[Brevo] FAILED: ${error.message}`);
        }
    }

    // Final Fallback: Console (Production Safety)
    console.log('--- CRITICAL EMAIL FALLBACK (CONSOLE ONLY) ---');
    console.log(`To: ${to}`);
    console.log(`Subject: ${subject}`);
    console.log(`HTML Body Snippet: ${html.substring(0, 100)}...`);
    console.log('----------------------------------------------');
    return { success: true, provider: 'console' };
};

module.exports = { sendMail };
