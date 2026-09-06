const db = require('../src/config/db');

async function testMerchant() {
    console.log('🧪 Testing Merchant Portal User Access...');

    try {
        const client = await db.pool.connect();
        try {
            await client.query('BEGIN');

            // 1. Setup a Test Merchant Account and a Sub-Account User
            const businessName = 'Test Bulk Merchant';
            const userEmail = `merchant_user_${Date.now()}@test.com`;

            const uRes = await client.query(
                "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES ($1, $2, $3, $4, $5) RETURNING id",
                ['Merchant User', userEmail, `+2349${Date.now().toString().slice(-9)}`, 'hash', 'CUSTOMER']
            );
            const userId = uRes.rows[0].id;

            const mRes = await client.query(
                "INSERT INTO merchant_accounts (business_name, contact_email, api_key_hash) VALUES ($1, $2, 'dummy_hash') RETURNING id",
                [businessName, userEmail]
            );
            const merchantId = mRes.rows[0].id;

            await client.query(
                "INSERT INTO merchant_sub_accounts (merchant_account_id, user_id, role) VALUES ($1, $2, 'admin')",
                [merchantId, userId]
            );

            // 2. Create a dummy batch
            const batchId = require('uuid').v4();
            await client.query(
                "INSERT INTO order_batches (id, merchant_account_id, name, total_orders, processed_orders, status) VALUES ($1, $2, $3, 10, 5, 'processing')",
                [batchId, merchantId, 'Sample Batch']
            );

            // 3. Simulate the controller logic for getMyBatches
            const { rows: batches } = await client.query(`
                SELECT b.*
                FROM order_batches b
                JOIN merchant_accounts ma ON ma.id = b.merchant_account_id
                JOIN merchant_sub_accounts msa ON msa.merchant_account_id = ma.id
                WHERE msa.user_id = $1
            `, [userId]);

            if (batches.length > 0 && batches[0].name === 'Sample Batch') {
                console.log(`✅ MERCHANT PASS: User ${userId} correctly retrieved batch "${batches[0].name}"`);
            } else {
                console.error(`❌ MERCHANT FAIL: Batch not found or incorrect.`);
                console.log('Batches found:', batches);
            }

            await client.query('ROLLBACK');
            process.exit(0);
        } finally {
            client.release();
        }
    } catch (error) {
        console.error('❌ TEST ERROR:', error.message);
        process.exit(1);
    }
}

testMerchant();
