const db = require('../src/config/db');
require('dotenv').config();

async function seed() {
    console.log('🎟️  SEEDING 100% DISCOUNT TEST COUPON...');

    try {
        const code = 'TEST100';
        const discountType = 'PERCENTAGE';
        const discountValue = 100.00;

        // Clean up existing if any
        await db.query("DELETE FROM coupons WHERE code = $1", [code]);

        // Insert new coupon
        await db.query(
            `INSERT INTO coupons (code, discount_type, discount_value, is_active, usage_limit)
             VALUES ($1, $2, $3, true, 9999)`,
            [code, discountType, discountValue]
        );

        console.log(`✅ Success! Use coupon code: ${code} for 100% discount.`);
        process.exit(0);
    } catch (e) {
        console.error('❌ Seeding failed:', e.message);
        process.exit(1);
    }
}

seed();
