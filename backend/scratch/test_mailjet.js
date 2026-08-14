const { sendMail } = require('../src/services/emailService');
require('dotenv').config({ path: '../.env' });

async function testMailjet() {
    console.log('--- Mailjet Diagnostic Tool ---');
    console.log('Checking configuration...');

    if (!process.env.MAILJET_API_KEY || !process.env.MAILJET_SECRET_KEY) {
        console.error('❌ ERROR: MAILJET_API_KEY or MAILJET_SECRET_KEY is missing in .env');
        process.exit(1);
    }

    const testEmail = process.env.EMAIL_FROM || 'awagroceries@gmail.com';
    const testSubject = "Pikop Mailjet Diagnostic Test";
    const testHtml = "<h1>Success!</h1><p>Your Mailjet integration is functional and ready for production.</p>";

    console.log(`Attempting to send test email to ${testEmail}...`);

    try {
        const result = await sendMail(testEmail, testSubject, testHtml);
        if (result.success) {
            console.log('✅ SUCCESS: Mailjet API handshake complete.');
            console.log('Message ID:', result.messageId);
        } else {
            console.error('❌ FAILURE: Mailjet returned an error.');
            console.error('Error Details:', result.error);

            if (result.error.includes('unauthorized')) {
                console.error('👉 TIP: Authentication failed. Verify your API Key and Secret Key.');
            } else if (result.error.includes('Sender')) {
                console.error('👉 TIP: The EMAIL_FROM address is not verified in your Mailjet account.');
            }
        }
    } catch (error) {
        console.error('❌ CRITICAL FAILURE: Request failed.');
        console.error(error.message);
    }
}

testMailjet();
