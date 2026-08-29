const db = require('../src/config/db');
const axios = require('axios');
require('dotenv').config();

async function check() {
    console.log('🔍 PIKOP V3 SYSTEM INTEGRITY DIAGNOSTIC (Final)');
    console.log('--- Checking core components ---');

    try {
        // 1. Verify DB Connection
        const dbRes = await db.query('SELECT NOW()');
        console.log('✅ Database: Connected (', dbRes.rows[0].now, ')');

        // 2. Seed TEST100 Coupon
        console.log('🎟️  Coupon: Ensuring TEST100 exists...');
        await db.query("DELETE FROM coupons WHERE code = 'TEST100'");
        await db.query(
            "INSERT INTO coupons (code, discount_type, discount_value, is_active, usage_limit) VALUES ('TEST100', 'PERCENTAGE', 100.00, true, 9999)"
        );
        console.log('✅ Coupon: TEST100 is live.');

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
        console.log('📧 Email: Testing SMTP Configuration...');
        const emailService = require('../src/services/emailService');
        const testRes = await emailService.sendMail('pikop.ng@gmail.com', 'System Diagnostic', '<h1>Integrity Check Passed</h1>');
        if (testRes.success) {
            console.log('✅ Email: SMTP Relay is functional.');
        } else {
            console.warn('❌ Email: SMTP Relay failed. Check your credentials in .env');
        }

        console.log('\n--- Checking 3rd-Party Integrations ---');

        // 5. Paystack Connectivity
        const psKey = (process.env.PAYSTACK_SECRET_KEY || '').trim();
        if (psKey) {
            console.log('💳 Paystack: Checking connection...');
            try {
                const psRes = await axios.get('https://api.paystack.co/balance', {
                    headers: { Authorization: `Bearer ${psKey}` }
                });
                console.log(`✅ Paystack: OK. Balance retrieved.`);
            } catch (e) {
                console.warn('❌ Paystack: FAIL. Error:', e.response?.data?.message || e.message);
            }
        }

        // 6. Prembly (Identitypass) Connectivity
        const prKey = (process.env.PREMBLY_SECRET_KEY || '').trim();
        if (prKey) {
            console.log('🆔 Prembly: Checking connection...');
            try {
                // Identitypass V2 Balance Check
                const prRes = await axios.get('https://api.prembly.com/identitypass/verification/account/balance', {
                    headers: { 'x-api-key': prKey }
                });
                console.log(`✅ Prembly: OK. Status: ${prRes.data.status || 'Active'}`);
            } catch (e) {
                console.warn('❌ Prembly: FAIL. Error:', e.response?.data?.message || e.message);
            }
        }

        // 7. Dojah Connectivity
        const djKey = (process.env.DOJAH_SECRET_KEY || '').trim();
        const djAppId = (process.env.DOJAH_APP_ID || '').trim();
        if (djKey && djAppId) {
            console.log('🆔 Dojah: Checking connection...');
            try {
                const djRes = await axios.get('https://api.dojah.io/api/v1/balance', {
                    headers: {
                        'Authorization': djKey,
                        'App-Id': djAppId
                    }
                });
                console.log(`✅ Dojah: OK. Wallet Balance: ${djRes.data.entity?.wallet_balance || 'N/A'}`);
            } catch (e) {
                console.warn('❌ Dojah: FAIL. Error:', e.response?.data?.error || e.message);
            }
        }

        console.log('\n✨ ALL CHECKS COMPLETED.');
        process.exit(0);
    } catch (e) {
        console.error('\n❌ CRITICAL SYSTEM ERROR:', e.message);
        process.exit(1);
    }
}

check();
