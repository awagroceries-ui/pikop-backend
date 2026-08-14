const emailService = require('../src/services/emailService');
require('dotenv').config({ path: __dirname + '/../.env' });

const testEmail = async () => {
    console.log('Testing Email Configuration...');
    console.log('SMTP_HOST:', process.env.SMTP_HOST);
    console.log('SMTP_PORT:', process.env.SMTP_PORT);
    console.log('SMTP_USER:', process.env.SMTP_USER);
    console.log('EMAIL_FROM:', process.env.EMAIL_FROM);

    const recipient = process.argv[2];
    if (!recipient) {
        console.error('Please provide a recipient email: node scratch/test_email.js your-email@example.com');
        process.exit(1);
    }

    try {
        const result = await emailService.sendMail(
            recipient,
            'Pikop Email Test',
            '<h1>Test Successful</h1><p>This is a test email from the Pikop server.</p>'
        );

        if (result.success) {
            console.log('✅ Email sent successfully! Message ID:', result.messageId);
        } else {
            console.error('❌ Email failed to send:', result.error);
        }
    } catch (error) {
        console.error('❌ Unexpected error during email test:', error.message);
    }
};

testEmail();
