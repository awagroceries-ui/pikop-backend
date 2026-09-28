const db = require('../src/config/db');
require('dotenv').config();

async function seed() {
    console.log('🎟️  SEEDING 100% DISCOUNT TEST COUPONS (TEST100 & TESTER100)...');

    const coupons = [
        { code: 'TEST100', scope: 'DELIVERY_ONLY', desc: '100% off Delivery Fee Only' },
        { code: 'TESTER100', scope: 'TOTAL_BILL', desc: '100% off Entire Checkout Bill' }
    ];

    try {
        // Ensure column exists first in case migration hasn't run yet
        await db.query(`ALTER TABLE coupons ADD COLUMN IF NOT EXISTS applicability_scope VARCHAR(50) NOT NULL DEFAULT 'DELIVERY_ONLY';`);

        for (const c of coupons) {
            // Clean up existing if any
            await db.query("DELETE FROM coupons WHERE code = $1", [c.code]);

            // Insert new coupon with designated scope, 0 min order and no expiry
            await db.query(
                `INSERT INTO coupons (code, discount_type, discount_value, is_active, usage_limit, min_order_amount, applicability_scope)
                 VALUES ($1, 'PERCENTAGE', 100.00, true, 9999, 0, $2)`,
                [c.code, c.scope]
            );
            console.log(`✅ Success! Coupon code '${c.code}' registered for 100% discount (${c.desc}). Scope: ${c.scope}`);
        }
        process.exit(0);
    } catch (e) {
        console.error('❌ Seeding failed:', e.message);
        process.exit(1);
    }
}

seed();
