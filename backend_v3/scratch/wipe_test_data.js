const db = require('../src/config/db');
require('dotenv').config();
const { execSync } = require('child_process');
const path = require('path');

async function wipe() {
    console.log('🧹 PIKOP V3 DATA WIPE INITIATED...');

    const tablesToClear = [
        'users',
        'fulfillers',
        'orders',
        'quotes',
        'addresses',
        'vehicles',
        'kyc_documents',
        'conversations',
        'messages',
        'wallets',
        'wallet_ledger_entries',
        'withdrawals',
        'referrals',
        'loyalty_ledger',
        'user_fcm_tokens',
        'notifications',
        'merchant_accounts',
        'merchant_sub_accounts',
        'order_batches'
    ];

    try {
        // 1. Perform Truncate
        console.log('🗑️  Truncating test data tables...');
        const truncateQuery = `TRUNCATE ${tablesToClear.join(', ')} CASCADE`;
        await db.query(truncateQuery);
        console.log('✅ All test user data tables cleared successfully.');

        // 2. Re-seed TEST100 Coupon
        console.log('🎟️  Re-seeding TEST100 coupon...');
        try {
            const seedScriptPath = path.join(__dirname, 'v3_seed_test_coupon.js');
            execSync(`node "${seedScriptPath}"`, { stdio: 'inherit' });
            console.log('✅ TEST100 coupon re-seeded.');
        } catch (seedErr) {
            console.warn('⚠️  Coupling seed failed, manual re-seed required:', seedErr.message);
        }

        console.log('\n✨ SYSTEM WIPED & READY FOR FRESH TESTING.');
        process.exit(0);
    } catch (e) {
        console.error('\n❌ CRITICAL WIPE FAILURE:', e.message);
        process.exit(1);
    }
}

wipe();
