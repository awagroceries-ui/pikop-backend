const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const fixConflict = async () => {
    console.log('🛠️ Fixing Modular Infrastructure Migration Conflict...');

    const pool = new Pool({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
    });

    try {
        // Remove any record of the redundant knowledge_base migration name
        console.log('- Cleaning up duplicate migration records...');
        await pool.query("DELETE FROM pgmigrations WHERE name = '1723680000000_knowledge_base'");

        console.log('✅ Conflict records cleared.');
        console.log('You can now run "npm run migrate up" with the updated code.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Fix Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

fixConflict();
