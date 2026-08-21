const db = require('../src/config/db');
const bcrypt = require('bcryptjs');
require('dotenv').config();

async function resetAdmin() {
    console.log('🛠️ Starting Admin Password Reset...');

    try {
        const username = 'admin';
        const password = 'pikop123';
        const saltRounds = 10;

        console.log(`[Reset] Generating hash for '${username}'...`);
        const hash = await bcrypt.hash(password, saltRounds);

        console.log(`[Reset] Deleting existing user '${username}' (if any)...`);
        await db.query("DELETE FROM admin_users WHERE username = $1", [username]);

        console.log(`[Reset] Creating fresh '${username}' user...`);
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin')",
            [username, hash]
        );

        console.log('✅ Admin password reset successfully to: pikop123');
        process.exit(0);
    } catch (error) {
        console.error('❌ Reset Failed:', error.message);
        process.exit(1);
    }
}

resetAdmin();
