const { Pool } = require('pg');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const fixDatabase = async () => {
    console.log('🚀 Starting Database Integrity Fix...');

    let connString = process.env.DATABASE_URL;

    // Simple but robust replacement for localhost/::1 to force IPv4
    // This avoids the complex URL parser that fails on special characters like '@'
    if (connString) {
        connString = connString.replace('localhost', '127.0.0.1').replace('[::1]', '127.0.0.1');
        console.log('  ℹ️  Forcing IPv4 (127.0.0.1) for connection string.');
    }

    const pool = new Pool({
        connectionString: connString,
    });

    try {
        // 1. Ensure all users have a role
        console.log('- Verifying User Roles...');
        await pool.query("UPDATE users SET role = 'CUSTOMER' WHERE role IS NULL");

        // 2. Ensure every user has a wallet
        console.log('- Syncing User Wallets...');
        await pool.query(`
            INSERT INTO wallets (owner_id, owner_type, balance)
            SELECT id, 'USER', 0 FROM users
            WHERE id NOT IN (SELECT owner_id FROM wallets WHERE owner_type = 'USER')
            AND role = 'CUSTOMER'
        `);

        // 3. Ensure every fulfiller has a wallet
        console.log('- Syncing Fulfiller Wallets...');
        await pool.query(`
            INSERT INTO wallets (owner_id, owner_type, balance)
            SELECT id, 'FULFILLER', 0 FROM fulfillers
            WHERE id NOT IN (SELECT owner_id FROM wallets WHERE owner_type = 'FULFILLER')
        `);

        // 4. Ensure Platform wallet exists
        console.log('- Verifying Platform Wallet...');
        await pool.query(`
            INSERT INTO wallets (owner_type, balance)
            SELECT 'PLATFORM', 0
            WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE owner_type = 'PLATFORM')
        `);

        console.log('✅ Database synchronized and ready.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Database Fix Failed!');
        console.error('Error Details:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

fixDatabase();
