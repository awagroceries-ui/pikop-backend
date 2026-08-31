const db = require('../src/config/db');
const bcrypt = require('bcryptjs');
require('dotenv').config();

async function restore() {
    console.log('🚀 PIKOP VPS SYSTEM ALIGNMENT INITIATED');

    try {
        // 1. Reset Admin Password (Ensures access even if DB was wiped)
        console.log('[1/2] Syncing Admin Credentials...');
        const hash = await bcrypt.hash('pikop123', 10);
        await db.query("DELETE FROM admin_users WHERE username = 'admin'");
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin')",
            ['admin', hash]
        );
        console.log('✅ Admin reset to: admin / pikop123');

        // 2. Validate Environment
        console.log('[2/2] Validating Critical Keys...');
        const keys = [
            'DATABASE_URL',
            'JWT_SECRET',
            'SMTP_USER',
            'SMTP_PASS',
            'FIREBASE_SERVICE_ACCOUNT',
            'PREMBLY_SECRET_KEY',
            'GEMINI_API_KEY'
        ];

        keys.forEach(key => {
            if (!process.env[key] || process.env[key].includes('your_')) {
                console.warn(`   ⚠️  WARNING: ${key} is missing or has placeholder value.`);
            } else {
                console.log(`   ✅ ${key} is configured.`);
            }
        });

        console.log('\n✨ ALIGNMENT COMPLETE. Please run: pm2 restart pikop-v3');
        process.exit(0);
    } catch (e) {
        console.error('\n❌ ALIGNMENT FAILED:', e.message);
        process.exit(1);
    }
}

restore();
