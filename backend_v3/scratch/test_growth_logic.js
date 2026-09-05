const db = require('../src/config/db');
const walletService = require('../src/services/walletService');

async function testGrowth() {
    console.log('🧪 Testing Referrals & Loyalty Logic...');

    try {
        const client = await db.pool.connect();
        try {
            await client.query('BEGIN');

            // 1. Setup Referrer and Referred User
            const referrerEmail = `referrer_${Date.now()}@test.com`;
            const referredEmail = `referred_${Date.now()}@test.com`;

            const r1 = await client.query(
                "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES ($1, $2, $3, $4, $5) RETURNING id",
                ['Referrer', referrerEmail, `+2341${Date.now().toString().slice(-9)}`, 'hash', 'CUSTOMER']
            );
            const referrerId = r1.rows[0].id;

            const r2 = await client.query(
                "INSERT INTO users (full_name, email, phone, password_hash, role, referred_by_user_id) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id",
                ['New User', referredEmail, `+2342${Date.now().toString().slice(-9)}`, 'hash', 'CUSTOMER', referrerId]
            );
            const referredId = r2.rows[0].id;

            // 2. Test Loyalty Points (₦1500 spending -> 15 points)
            console.log(`[Test] Awarding points for ₦1500 spending...`);
            await walletService.awardLoyaltyPoints(client, referredId, 1500);
            const pointCheck = await client.query("SELECT points FROM loyalty_ledger WHERE user_id = $1", [referredId]);
            if (parseInt(pointCheck.rows[0].points) === 15) {
                console.log(`✅ LOYALTY PASS: Awarded 15 points.`);
            } else {
                console.error(`❌ LOYALTY FAIL: Expected 15, got ${pointCheck.rows[0].points}`);
            }

            // 3. Test Referral Reward
            console.log(`[Test] Processing referral reward...`);
            await walletService.processReferralReward(client, referredId);

            const refCheck = await client.query("SELECT status FROM referrals WHERE referred_id = $1", [referredId]);
            const wallet1 = await client.query("SELECT balance FROM wallets WHERE owner_id = $1", [referrerId.toString()]);
            const wallet2 = await client.query("SELECT balance FROM wallets WHERE owner_id = $1", [referredId.toString()]);

            if (refCheck.rows[0]?.status === 'completed' && parseFloat(wallet1.rows[0].balance) === 250 && parseFloat(wallet2.rows[0].balance) === 250) {
                console.log(`✅ REFERRAL PASS: Both users received ₦250 bonus.`);
            } else {
                console.error(`❌ REFERRAL FAIL: Bonuses not applied correctly.`);
                console.log('Referral:', refCheck.rows[0]);
                console.log('Referrer Balance:', wallet1.rows[0]?.balance);
                console.log('User Balance:', wallet2.rows[0]?.balance);
            }

            await client.query('ROLLBACK');
            console.log('\nResult: All growth tests completed.');
            process.exit(0);
        } finally {
            client.release();
        }
    } catch (error) {
        console.error('❌ TEST ERROR:', error.message);
        process.exit(1);
    }
}

testGrowth();
