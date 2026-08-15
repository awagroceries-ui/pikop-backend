const { sendMail } = require('../src/services/emailService');
require('dotenv').config({ path: '../.env' });

async function testBrevo() {
    console.log('--- Brevo SMTP Diagnostic Tool ---');
    console.log('Checking configuration...');

    if (!process.env.SMTP_USER || !process.env.SMTP_PASS) {
        console.error('❌ ERROR: SMTP_USER or SMTP_PASS is missing in .env');
        process.exit(1);
    }

    const testEmail = process.env.EMAIL_FROM || 'awagroceries@gmail.com';
    const testSubject = "Pikop Brevo Restoration Test";
    const testHtml = "<h1>Success!</h1><p>Brevo SMTP has been restored as the primary email provider for Pikop.</p>";

    console.log(`Attempting to send test email to ${testEmail}...`);

    try {
        const result = await sendMail(testEmail, testSubject, testHtml);
        if (result.messageId !== 'console-fallback') {
            console.log('✅ SUCCESS: Brevo SMTP delivery confirmed.');
            console.log('Message ID:', result.messageId);
        } else {
            console.warn('⚠️ WARNING: Delivery fell back to Console. Check if credentials are correct.');
        }
    } catch (error) {
        console.error('❌ CRITICAL FAILURE: Request failed.');
        console.error(error.message);
    }
}

testBrevo();
