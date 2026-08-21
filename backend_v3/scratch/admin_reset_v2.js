const db = require('../src/config/db');
const bcrypt = require('bcryptjs');
require('dotenv').config();

async function reset() {
    console.log('🔄 Re-generating Admin Credentials...');
    try {
        const username = 'admin';
        const pass = 'pikop123';

        console.log(`[Reset] Hashing password for '${username}'...`);
        const hash = await bcrypt.hash(pass, 10);

        console.log('[Reset] Deleting existing entries...');
        await db.query("DELETE FROM admin_users WHERE username = $1", [username]);

        console.log('[Reset] Inserting fresh super_admin...');
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin')",
            [username, hash]
        );

        console.log('✅ Admin reset to: admin / pikop123');
        console.log('✅ Generated Hash:', hash);
        process.exit(0);
    } catch (e) {
        console.error('❌ Reset failed:', e.message);
        process.exit(1);
    }
}
reset();
