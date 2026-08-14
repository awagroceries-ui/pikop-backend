const nodemailer = require('nodemailer');
require('dotenv').config({ path: '../.env' });

async fun testSmtp() {
    console.log('--- SMTP Diagnostic Tool ---');
    console.log('Checking configuration...');

    if (!process.env.SMTP_USER || !process.env.SMTP_PASS) {
        console.error('❌ ERROR: SMTP_USER or SMTP_PASS is missing in .env');
        process.exit(1);
    }

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

    console.log('Attempting handshake...');
    try {
        await transporter.verify();
        console.log('✅ SUCCESS: Handshake completed. SMTP credentials are valid.');

        console.log('Sending test email to verify sender status...');
        const info = await transporter.sendMail({
            from: process.env.EMAIL_FROM || '"Pikop Test" <awagroceries@gmail.com>',
            to: process.env.SMTP_USER, // Send to self
            subject: "Pikop SMTP Test",
            text: "If you received this, your email delivery is fully functional."
        });
        console.log('✅ SUCCESS: Test email sent. MessageId:', info.messageId);
    } catch (error) {
        console.error('❌ FAILURE: SMTP connection failed.');
        console.error('Error Code:', error.code);
        console.error('Error Message:', error.message);

        if (error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT') {
            console.error('👉 TIP: Port 587 seems blocked by your VPS provider. Try asking them to unblock SMTP traffic.');
        } else if (error.message.includes('535')) {
            console.error('👉 TIP: Authentication failed. Double-check your Login ID and SMTP Key in Brevo.');
        } else if (error.message.includes('Sender address rejected')) {
            console.error('👉 TIP: The EMAIL_FROM address is not verified in your Brevo account.');
        }
    }
}

testSmtp();
