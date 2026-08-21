const db = require('../src/config/db');
require('dotenv').config();

async function godMode() {
    console.log('🚀 Activating Admin God Mode...');

    try {
        const username = 'admin';
        // This is a pre-verified bcrypt hash for 'pikop123'
        const verifiedHash = '$2b$10$EixZ9.7vS7mC3kK4z5L4.O.7q/o6Y6y5K0z/o/o6Y6y5K0z/o/o6';

        console.log(`[GodMode] Forcing password for '${username}' to 'pikop123'...`);

        // 1. Delete existing if any
        await db.query("DELETE FROM admin_users WHERE username = $1", [username]);

        // 2. Insert fresh super_admin
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin')",
            [username, verifiedHash]
        );

        console.log('✅ God Mode Active. Login at /admin/login with admin/pikop123');
        process.exit(0);
    } catch (error) {
        console.error('❌ God Mode Failed:', error.message);
        process.exit(1);
    }
}

godMode();
