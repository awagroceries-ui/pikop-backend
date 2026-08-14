const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const fixDb = async () => {
    console.log('🛠️ Starting VPS Database Recovery...');

    const pool = new Pool({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
    });

    try {
        console.log('- Cleaning up broken migration history...');
        // Remove the record for the missing kyc_documents file that was blocking migrations
        await pool.query("DELETE FROM pgmigrations WHERE name = '1723060000000_kyc_documents'");

        console.log('- Injecting missing columns into orders table...');
        // Manually add rating columns if they don't exist
        await pool.query(`
            ALTER TABLE orders
            ADD COLUMN IF NOT EXISTS rating INTEGER CHECK (rating >= 1 AND rating <= 5),
            ADD COLUMN IF NOT EXISTS rating_comment TEXT
        `);

        console.log('✅ VPS Database fixed successfully.');
        console.log('You can now run "npm run migrate up" without errors.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Recovery Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

fixDb();
