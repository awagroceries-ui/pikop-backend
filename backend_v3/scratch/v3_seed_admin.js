const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
require('dotenv').config({ path: '../.env' });

const seedAdmin = async () => {
    console.log('🚀 Seeding V3 Super Admin...');

    const pool = new Pool({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
    });

    const username = 'admin';
    const password = 'pikop_password_2026'; // Change immediately

    try {
        const hash = await bcrypt.hash(password, 10);
        await pool.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin') ON CONFLICT (username) DO NOTHING",
            [username, hash]
        );

        // Add default settings
        await pool.query("INSERT INTO settings (key, value) VALUES ('base_fare_small', '500') ON CONFLICT DO NOTHING");
        await pool.query("INSERT INTO settings (key, value) VALUES ('base_fare_medium', '1000') ON CONFLICT DO NOTHING");
        await pool.query("INSERT INTO settings (key, value) VALUES ('base_fare_large', '1500') ON CONFLICT DO NOTHING");
        await pool.query("INSERT INTO settings (key, value) VALUES ('per_km_rate', '150') ON CONFLICT DO NOTHING");

        console.log('✅ Admin Created Successfully!');
        console.log(`User: ${username}`);
        console.log(`Pass: ${password}`);
        process.exit(0);
    } catch (error) {
        console.error('❌ Seeding Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

seedAdmin();
