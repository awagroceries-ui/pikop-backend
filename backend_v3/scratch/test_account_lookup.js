const db = require('../src/config/db');
const { normalizePhone } = require('../src/utils/phone');

async function testLookup() {
    console.log('🧪 Testing Account Lookup Branching...');

    try {
        // 1. Create a test user with normalized phone
        const testPhone = '09011223344';
        const normalized = normalizePhone(testPhone);

        await db.query("DELETE FROM users WHERE phone = $1", [normalized]);
        await db.query(
            "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES ($1, $2, $3, $4, $5)",
            ['Lookup Tester', 'lookup@test.com', normalized, 'hash', 'CUSTOMER']
        );
        console.log(`[Test] Created user with phone: ${normalized}`);

        // 2. Perform lookup with different formats
        const formats = [testPhone, '+2349011223344', '234 901 122 3344'];

        for (const fmt of formats) {
            const normInput = normalizePhone(fmt);
            const { rows } = await db.query("SELECT id FROM users WHERE phone = $1", [normInput]);

            if (rows.length > 0) {
                console.log(`✅ MATCH SUCCESS: Input "${fmt}" (normalized: ${normInput}) correctly matched User ID ${rows[0].id}`);
            } else {
                console.error(`❌ MATCH FAILED: Input "${fmt}" (normalized: ${normInput}) did not match existing user`);
            }
        }

        // 3. Test non-existent number
        const guestPhone = '07000000000';
        const normGuest = normalizePhone(guestPhone);
        const guestMatch = await db.query("SELECT id FROM users WHERE phone = $1", [normGuest]);

        if (guestMatch.rows.length === 0) {
            console.log(`✅ GUEST FLOW: Number ${guestPhone} correctly identified as Guest.`);
        } else {
            console.error(`❌ GUEST FAILED: Number ${guestPhone} unexpectedly matched a user.`);
        }

        process.exit(0);
    } catch (error) {
        console.error('❌ TEST ERROR:', error.message);
        process.exit(1);
    }
}

testLookup();
