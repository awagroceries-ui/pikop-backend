const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const testEmail = async () => {
    console.log('--- Pikop Email Diagnostic ---');
    console.log('SMTP Host:', process.env.SMTP_HOST);
    console.log('SMTP Port:', process.env.SMTP_PORT);
    console.log('SMTP User:', process.env.SMTP_USER);
    console.log('SMTP Pass Provided:', process.env.SMTP_PASS ? 'YES' : 'NO');
    console.log('------------------------------');

    const transporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST || 'smtp-relay.brevo.com',
        port: parseInt(process.env.SMTP_PORT || '587'),
        secure: false,
        auth: {
            user: process.env.SMTP_USER,
            pass: process.env.SMTP_PASS,
        },
        logger: true,
        debug: true
    });

    console.log('Verifying connection...');
    try {
        await transporter.verify();
        console.log('✅ SMTP Connection Successful!');

        console.log('Sending test email to:', process.env.SMTP_USER);
        const info = await transporter.sendMail({
            from: process.env.EMAIL_FROM || '"Pikop Test" <awagroceries@gmail.com>',
            to: process.env.SMTP_USER,
            subject: 'Pikop Email Diagnostic Test',
            text: 'If you are reading this, your SMTP configuration is working perfectly!',
            html: '<b>Success!</b> Your SMTP configuration is working perfectly.'
        });

        console.log('✅ Test email sent! Message ID:', info.messageId);
        process.exit(0);
    } catch (error) {
        console.error('❌ Diagnostic Failed!');
        console.error('Error Message:', error.message);
        console.error('Full Error Stack:', error);

        if (error.code === 'ECONNTIMEOUT') {
            console.log('\nTIP: Port 587 seems blocked by your VPS. Try using Port 465 or contact TrueHost support.');
        }
        process.exit(1);
    }
};

testEmail();
