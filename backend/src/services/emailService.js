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

  try {
    const payload = {
      Messages: [
        {
          From: {
            Email: EMAIL_FROM,
            Name: "Pikop"
          },
          To: [
            {
              Email: to
            }
          ],
          Subject: subject,
          HTMLPart: html,
          TextPart: html.replace(/<[^>]*>?/gm, '') // Simple HTML to Text conversion
        }
      ]
    };

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
    console.log(`[Mailjet] Success: Email sent to ${to}. ID: ${messageId}`);
    return { success: true, messageId };

  } catch (error) {
    console.error(`[Mailjet] Failure: Failed to send to ${to}`);

    if (error.response) {
      // The request was made and the server responded with a status code
      // that falls out of the range of 2xx
      console.error('[Mailjet] API Error:', error.response.data);
      console.error('[Mailjet] Status:', error.response.status);

      const errorDetail = error.response.data.Messages ? error.response.data.Messages[0].Errors : error.response.data;
      return { success: false, error: JSON.stringify(errorDetail) };
    } else if (error.request) {
      // The request was made but no response was received
      console.error('[Mailjet] No response received from server.');
      return { success: false, error: 'No response from Mailjet' };
    } else {
      // Something happened in setting up the request that triggered an Error
      console.error('[Mailjet] Request Setup Error:', error.message);
      return { success: false, error: error.message };
    }
  }
};

module.exports = {
  sendMail
};
