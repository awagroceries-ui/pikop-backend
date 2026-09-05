const db = require('../src/config/db');
require('dotenv').config();

async function verify() {
    console.log('🔍 VERIFYING STABILIZATION FIXES...');

    try {
        // 1. Check Orders Table Columns
        const orderColsRes = await db.query(`
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = 'orders'
            AND column_name IN ('pod_photo_url', 'delivered_at')
        `);
        console.log('[Verify] Orders Table Columns:', orderColsRes.rows);

        // 2. Check Fulfillers Table Columns
        const fulfillerColsRes = await db.query(`
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = 'fulfillers'
            AND column_name IN ('kyc_details', 'didit_verification_status')
        `);
        console.log('[Verify] Fulfiller Table Columns:', fulfillerColsRes.rows);

        // 3. Test Controller Logic Robustness (Syntactic Check)
        const orderController = require('../src/controllers/orderController');
        if (typeof orderController.verifyDelivery === 'function') {
            console.log('✅ verifyDelivery controller exported.');
        }

        console.log('\n--- VERIFICATION SUMMARY ---');
        const hasOrderCols = orderColsRes.rows.length === 2;
        const hasKycDetails = fulfillerColsRes.rows.some(r => r.column_name === 'kyc_details');

        if (hasOrderCols) console.log('✅ POD columns confirmed.');
        else console.warn('⚠️ Missing POD columns. Run migration 1725570000000_stabilize_v3_schema.js');

        if (hasKycDetails) console.log('✅ KYC details column confirmed.');
        else console.warn('⚠️ Missing kyc_details column. Run migration 1725570000000_stabilize_v3_schema.js');

        process.exit(0);
    } catch (error) {
        console.error('❌ VERIFICATION FAILED:', error.message);
        process.exit(1);
    }
}

verify();
