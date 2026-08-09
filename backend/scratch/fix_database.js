const { Client } = require('pg');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const fixDatabase = async () => {
    console.log('🚀 Starting Database Integrity Fix (Robust Mode)...');

    // Use connection object to avoid parsing issues with special characters in URL
    const client = new Client({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1')
    });

    try {
        await client.connect();
        console.log('  ✅ Connected to database.');

        // 1. Ensure all users have a role
        console.log('- Verifying User Roles...');
        await client.query("UPDATE users SET role = 'CUSTOMER' WHERE role IS NULL");

        // 2. Ensure every user has a wallet
        console.log('- Syncing User Wallets...');
        await client.query(`
            INSERT INTO wallets (owner_id, owner_type, balance)
            SELECT id, 'USER', 0 FROM users
            WHERE id NOT IN (SELECT owner_id FROM wallets WHERE owner_type = 'USER')
            AND role = 'CUSTOMER'
        `);

        // 3. Ensure every fulfiller has a wallet
        console.log('- Syncing Fulfiller Wallets...');
        await client.query(`
            INSERT INTO wallets (owner_id, owner_type, balance)
            SELECT id, 'FULFILLER', 0 FROM fulfillers
            WHERE id NOT IN (SELECT owner_id FROM wallets WHERE owner_type = 'FULFILLER')
        `);

        // 4. Ensure Platform wallet exists
        console.log('- Verifying Platform Wallet...');
        await client.query(`
            INSERT INTO wallets (owner_type, balance)
            SELECT 'PLATFORM', 0
            WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE owner_type = 'PLATFORM')
        `);

        console.log('✅ Database synchronized and ready.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Database Fix Failed:', error.message);
        process.exit(1);
    } finally {
        await client.end();
    }
};

fixDatabase();
