const db = require('../src/config/db');
require('dotenv').config();

async function check() {
    console.log('🔍 PIKOP V3 SYSTEM INTEGRITY DIAGNOSTIC');

    try {
        // 1. Verify DB Connection
        const dbRes = await db.query('SELECT NOW()');
        console.log('✅ Database Connected:', dbRes.rows[0].now);

        // 2. Seed TEST100 Coupon
        console.log('🎟️  Ensuring TEST100 coupon exists...');
        await db.query("DELETE FROM coupons WHERE code = 'TEST100'");
        await db.query(
            "INSERT INTO coupons (code, discount_type, discount_value, is_active, usage_limit) VALUES ('TEST100', 'PERCENTAGE', 100.00, true, 9999)"
        );
        console.log('✅ Coupon TEST100 is live.');

        // 3. Check Messages Schema
        console.log('💬 Checking messaging schema...');
        const colRes = await db.query("SELECT column_name FROM information_schema.columns WHERE table_name = 'messages'");
        const columns = colRes.rows.map(r => r.column_name);
        if (!columns.includes('is_read')) {
            console.log('⚠️  Adding is_read to messages...');
            await db.query('ALTER TABLE messages ADD COLUMN is_read BOOLEAN DEFAULT false');
        }
        console.log('✅ Messaging schema is healthy.');

        // 4. SMTP Connectivity Test
        console.log('📧 Testing SMTP Configuration...');
        const emailService = require('../src/services/emailService');
        const testRes = await emailService.sendMail('pikop.ng@gmail.com', 'System Diagnostic', '<h1>Integrity Check Passed</h1>');
        if (testRes.success) {
            console.log('✅ SMTP Relay is functional.');
        } else {
            console.warn('❌ SMTP Relay failed. Check your credentials in .env');
        }

        console.log('\n✨ INTEGRITY CHECK COMPLETE.');
        process.exit(0);
    } catch (e) {
        console.error('❌ FATAL ERROR during integrity check:', e.message);
        process.exit(1);
    }
}

check();
