const axios = require('axios');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const MAILJET_API_KEY = process.env.MAILJET_API_KEY;
const MAILJET_SECRET_KEY = process.env.MAILJET_SECRET_KEY;
const EMAIL_FROM = process.env.EMAIL_FROM || 'awagroceries@gmail.com';

// Log status on startup
if (MAILJET_API_KEY && MAILJET_SECRET_KEY) {
    console.log('✅ Mailjet Email Service configured.');
} else {
    console.warn('⚠️ Mailjet configuration missing. Emails will be logged to console only.');
}

/**
 * Sends an email using Mailjet's v3.1 Send API.
 * @param {string} to - Recipient email address.
 * @param {string} subject - Email subject.
 * @param {string} html - HTML content of the email.
 */
const sendMail = async (to, subject, html) => {
  if (!MAILJET_API_KEY || !MAILJET_SECRET_KEY) {
    console.log('--- MOCK EMAIL (Mailjet Not Configured) ---');
    console.log('To:', to);
    console.log('Subject:', subject);
    console.log('-------------------------------------------');
    return { success: true, messageId: 'mock-id' };
  }

  // Clean the sender address (Remove quotes and shell-induced garbage)
  const cleanFrom = EMAIL_FROM.replace(/["'<>]/g, '').trim();

  try {
    const payload = {
      Messages: [
        {
          From: {
            Email: cleanFrom,
            Name: "Pikop Support"
          },
          To: [
            {
              Email: to
            }
          ],
          Subject: subject,
          HTMLPart: html,
          TextPart: html.replace(/<[^>]*>?/gm, '')
        }
      ]
    };

    console.log(`[Mailjet] Dispatching to ${to} from ${cleanFrom}...`);

    const response = await axios.post(
      'https://api.mailjet.com/v3.1/send',
      payload,
      {
        auth: {
          username: MAILJET_API_KEY,
          password: MAILJET_SECRET_KEY
        },
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );

    const messageId = response.data.Messages[0].To[0].MessageID;
    console.log(`[Mailjet] SUCCESS: Message delivered. ID: ${messageId}`);
    return { success: true, messageId };

  } catch (error) {
    console.error(`[Mailjet] FAILURE: Failed to send to ${to}`);

    if (error.response) {
      const errorData = error.response.data;
      console.error('[Mailjet] API Status:', error.response.status);
      console.error('[Mailjet] Error Details:', JSON.stringify(errorData));

      const errorMessage = errorData.Messages ? JSON.stringify(errorData.Messages[0].Errors) : JSON.stringify(errorData);
      return { success: false, error: errorMessage };
    } else {
      console.error('[Mailjet] Request Error:', error.message);
      return { success: false, error: error.message };
    }
  }
};

module.exports = {
  sendMail
};
