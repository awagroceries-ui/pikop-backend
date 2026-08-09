const { Client } = require('pg');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const debugDb = async () => {
    console.log('--- Database Debug ---');
    console.log('DATABASE_URL:', process.env.DATABASE_URL);

    // Attempt to connect as superuser to list roles
    const url = require('url');
    let connString = process.env.DATABASE_URL;
    if (connString && connString.includes('localhost')) {
        connString = connString.replace('localhost', '127.0.0.1');
    }

    const client = new Client({
        connectionString: connString,
    });

    try {
        console.log('Connecting...');
        await client.connect();
        console.log('✅ Connected successfully!');

        console.log('Checking current user...');
        const userRes = await client.query('SELECT current_user, current_database()');
        console.log('  Current User:', userRes.rows[0].current_user);
        console.log('  Current DB:', userRes.rows[0].current_database);

        console.log('Listing all roles in database:');
        const rolesRes = await client.query('SELECT rolname FROM pg_roles');
        rolesRes.rows.forEach(r => console.log(`  - ${r.rolname}`));

    } catch (error) {
        console.error('❌ Connection Failed:', error.message);
        console.error('Error Code:', error.code);
    } finally {
        await client.end();
    }
};

debugDb();
