const db = require('../src/config/db');

const fixDatabase = async () => {
    console.log('🚀 Starting Database Integrity Fix...');

    try {
        // 1. Ensure all users have a role
        console.log('- Verifying User Roles...');
        await db.query("UPDATE users SET role = 'CUSTOMER' WHERE role IS NULL");

        // 2. Ensure every user has a wallet
        console.log('- Syncing User Wallets...');
        await db.query(`
            INSERT INTO wallets (owner_id, owner_type, balance)
            SELECT id, 'USER', 0 FROM users
            WHERE id NOT IN (SELECT owner_id FROM wallets WHERE owner_type = 'USER')
            AND role = 'CUSTOMER'
        `);

        // 3. Ensure every fulfiller has a wallet
        console.log('- Syncing Fulfiller Wallets...');
        await db.query(`
            INSERT INTO wallets (owner_id, owner_type, balance)
            SELECT id, 'FULFILLER', 0 FROM fulfillers
            WHERE id NOT IN (SELECT owner_id FROM wallets WHERE owner_type = 'FULFILLER')
        `);

        // 4. Ensure Platform wallet exists
        console.log('- Verifying Platform Wallet...');
        await db.query(`
            INSERT INTO wallets (owner_type, balance)
            SELECT 'PLATFORM', 0
            WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE owner_type = 'PLATFORM')
        `);

        console.log('✅ Database synchronized and ready.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Database Fix Failed:', error.message);
        process.exit(1);
    }
};

fixDatabase();
