const db = require('../src/config/db');
require('dotenv').config();

async function seed() {
    console.log('🎟️  SEEDING 100% DISCOUNT TEST COUPONS (TEST100 & TESTER100)...');

    const codes = ['TEST100', 'TESTER100'];

    try {
        for (const code of codes) {
            // Clean up existing if any
            await db.query("DELETE FROM coupons WHERE code = $1", [code]);

            // Insert new coupon with 0 min order and no expiry
            await db.query(
                `INSERT INTO coupons (code, discount_type, discount_value, is_active, usage_limit, min_order_amount)
                 VALUES ($1, 'PERCENTAGE', 100.00, true, 9999, 0)`,
                [code]
            );
            console.log(`✅ Success! Coupon code '${code}' registered for 100% discount.`);
        }
        process.exit(0);
    } catch (e) {
        console.error('❌ Seeding failed:', e.message);
        process.exit(1);
    }
}

seed();
