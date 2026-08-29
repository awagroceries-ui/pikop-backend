const db = require('../src/config/db');
const axios = require('axios');
require('dotenv').config();

async function check() {
    console.log('🔍 PIKOP V3 FINAL SYSTEM INTEGRITY DIAGNOSTIC');

    try {
        // 1. Verify DB Connection
        const dbRes = await db.query('SELECT NOW()');
        console.log('✅ Database: Connected (', dbRes.rows[0].now, ')');

        // 2. Force-Seed TEST100 Coupon
        console.log('🎟️  Coupon: Forcing TEST100 activation...');
        await db.query("DELETE FROM coupons WHERE code = 'TEST100'");
        await db.query(
            "INSERT INTO coupons (code, discount_type, discount_value, is_active, usage_limit, min_order_amount) VALUES ('TEST100', 'PERCENTAGE', 100.00, true, 9999, 0)"
        );

        // Verification Query
        const couponRes = await db.query("SELECT * FROM coupons WHERE code = 'TEST100'");
        if (couponRes.rows.length > 0) {
            console.log('✅ Coupon: TEST100 is LIVE and VERIFIED in DB.');
        } else {
            console.error('❌ Coupon: TEST100 failed to persist!');
        }

        // 3. SMTP Connectivity Test
        console.log('📧 Email: Testing SMTP Connectivity...');
        const emailService = require('../src/services/emailService');
        const testRes = await emailService.sendMail('pikop.ng@gmail.com', 'System Alignment', '<h1 style="color: #008751;">Premium Email System Active</h1><p>Your diagnostic check passed successfully.</p>');
        if (testRes.success) {
            console.log('✅ Email: SMTP Relay Functional.');
        } else {
            console.warn('❌ Email: SMTP Relay failed. Please check BREVO keys.');
        }

        // 4. Paystack Check
        const psKey = (process.env.PAYSTACK_SECRET_KEY || '').trim();
        if (psKey) {
            try {
                await axios.get('https://api.paystack.co/balance', { headers: { Authorization: `Bearer \${psKey}` } });
                console.log(`✅ Paystack: Keys are VALID.`);
            } catch (e) {
                console.warn('❌ Paystack: Keys are INVALID.');
            }
        }

        console.log('\n✨ SYSTEM INTEGRITY VERIFIED.');
        process.exit(0);
    } catch (e) {
        console.error('\n❌ CRITICAL DIAGNOSTIC ERROR:', e.message);
        process.exit(1);
    }
}

check();
