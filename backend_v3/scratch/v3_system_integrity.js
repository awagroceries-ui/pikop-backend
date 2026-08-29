const db = require('../src/config/db');
const axios = require('axios');
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

        // 5. Paystack Connectivity
        console.log('💳 Checking Paystack Integration...');
        const psKey = process.env.PAYSTACK_SECRET_KEY;
        if (psKey) {
            try {
                const psRes = await axios.get('https://api.paystack.co/balance', {
                    headers: { Authorization: `Bearer ${psKey.trim()}` }
                });
                console.log(`✅ Paystack: Authenticated. Balance: ${psRes.data.data[0]?.currency} ${psRes.data.data[0]?.balance / 100}`);
            } catch (e) {
                console.warn('❌ Paystack: Authentication failed. Check PAYSTACK_SECRET_KEY.');
            }
        } else {
            console.warn('⚠️  Paystack: PAYSTACK_SECRET_KEY missing in .env.');
        }

        // 6. Prembly Connectivity
        console.log('🆔 Checking Prembly Integration...');
        const prKey = process.env.PREMBLY_SECRET_KEY;
        if (prKey) {
            try {
                const prRes = await axios.get('https://api.prembly.com/identitypass/verification/account/balance', {
                    headers: { 'x-api-key': prKey.trim() }
                });
                console.log(`✅ Prembly: Authenticated. Status: ${prRes.data.status}`);
            } catch (e) {
                console.warn('❌ Prembly: Authentication failed. Check PREMBLY_SECRET_KEY.');
            }
        } else {
            console.warn('⚠️  Prembly: PREMBLY_SECRET_KEY missing in .env.');
        }

        // 7. Dojah Connectivity
        console.log('🆔 Checking Dojah Integration...');
        const djKey = process.env.DIDIT_API_KEY; // Reusing key as mapped in provider
        const djAppId = process.env.DOJAH_APP_ID;
        if (djKey && djAppId) {
            try {
                const djRes = await axios.get('https://api.dojah.io/api/v1/balance', {
                    headers: { Authorization: djKey.trim(), 'App-Id': djAppId.trim() }
                });
                console.log(`✅ Dojah: Authenticated. Wallet: ${djRes.data.entity.wallet_balance}`);
            } catch (e) {
                console.warn('❌ Dojah: Authentication failed. Check DOJAH keys.');
            }
        } else {
            console.warn('⚠️  Dojah: Credentials missing in .env.');
        }

        console.log('\n✨ INTEGRITY CHECK COMPLETE.');
        process.exit(0);
    } catch (e) {
        console.error('❌ FATAL ERROR during integrity check:', e.message);
        process.exit(1);
    }
}

check();
