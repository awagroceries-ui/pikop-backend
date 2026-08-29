const db = require('../src/config/db');
require('dotenv').config();

async function seed() {
    console.log('🎟️  SEEDING 100% DISCOUNT TEST COUPON...');

    try {
        const code = 'TEST100';
        const discountType = 'PERCENTAGE';
        const discountValue = 100.00;

        // Ensure table exists (pre-migration check)
        await db.query(`
            CREATE TABLE IF NOT EXISTS coupons (
                id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                code varchar(50) UNIQUE NOT NULL,
                discount_type varchar(20) NOT NULL,
                discount_value decimal(12,2) NOT NULL,
                min_order_amount decimal(12,2) DEFAULT 0,
                is_active boolean DEFAULT true,
                usage_limit integer DEFAULT 9999,
                usage_count integer DEFAULT 0,
                created_at timestamp DEFAULT current_timestamp
            )
        `);

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
