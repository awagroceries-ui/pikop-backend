const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const seedGrowth = async () => {
    console.log('🚀 Seeding V3 Growth & Coupons...');

    const pool = new Pool({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
    });

    try {
        // 1. Create Default Coupon
        await pool.query(
            "INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, is_active) VALUES ($1, $2, $3, $4, true) ON CONFLICT DO NOTHING",
            ['PIKOP500', 'FIXED', 500, 2000]
        );

        console.log('✅ Growth Seeded.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Seeding Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

seedGrowth();
