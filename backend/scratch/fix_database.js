const { Pool } = require('pg');
const path = require('path');
const url = require('url');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const fixDatabase = async () => {
    console.log('🚀 Starting Database Integrity Fix...');

    let connString = process.env.DATABASE_URL;

    // Robust URL parsing to handle special characters and force IPv4
    try {
        const parsedUrl = new url.URL(connString);
        parsedUrl.hostname = '127.0.0.1'; // Force IPv4
        connString = parsedUrl.toString();
        console.log('  ℹ️  Forcing IPv4 (127.0.0.1) for local connection.');
    } catch (e) {
        console.warn('  ⚠️  Could not parse DATABASE_URL, using original string.');
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
        if (error.message.includes('role')) {
            console.log('\nTIP: Your database user might be misconfigured. Verify the username and password in .env.');
        }
        process.exit(1);
    } finally {
        await pool.end();
    }
};

fixDatabase();
