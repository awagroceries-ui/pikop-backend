const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const path = require('path');

// Master Reset Seeder - v3.0.7
// Hardcoded path to .env to ensure loading regardless of execution context
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const seedAdmin = async () => {
    console.log('🚀 Seeding V3 Super Admin...');

    const dbUrl = process.env.DATABASE_URL;
    if (!dbUrl) {
        console.error('❌ ERROR: DATABASE_URL not found in .env');
        process.exit(1);
    }

    const pool = new Pool({
        connectionString: dbUrl.replace('localhost', '127.0.0.1'),
    });

    const username = 'admin';
    const password = 'pikop_password_2026';

    try {
        const hash = await bcrypt.hash(password, 10);

        // 1. Create Admin
        await pool.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin') ON CONFLICT (username) DO NOTHING",
            [username, hash]
        );

        // 2. Default Platform Settings
        const settings = [
            ['base_fare_small', '500'],
            ['base_fare_medium', '1000'],
            ['base_fare_large', '1500'],
            ['per_km_rate', '150'],
            ['platform_commission', '0.25']
        ];

        for (const [key, val] of settings) {
            await pool.query(
                "INSERT INTO settings (key, value) VALUES ($1, $2) ON CONFLICT (key) DO UPDATE SET value = $2",
                [key, val]
            );
        }

        console.log('✅ V3 Admin & Settings Seeded Successfully!');
        console.log(`URL: ${dbUrl.substring(0, 20)}...`);
        console.log(`User: ${username} | Pass: ${password}`);
        process.exit(0);
    } catch (error) {
        console.error('❌ Seeding Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

seedAdmin();
