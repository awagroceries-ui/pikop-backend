const db = require('../src/config/db');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const checkSystem = async () => {
    console.log('\n🔍 --- Pikop System Diagnostic ---');

    // 1. Check Environment
    console.log('\n[1/3] Environment Verification:');
    const requiredKeys = ['DATABASE_URL', 'JWT_SECRET', 'REFRESH_TOKEN_SECRET', 'GEMINI_API_KEY', 'PAYSTACK_SECRET_KEY', 'FIREBASE_SERVICE_ACCOUNT'];
    requiredKeys.forEach(key => {
        const val = process.env[key];
        if (!val) {
            console.error(`  ❌ Missing Key: ${key}`);
        } else {
            console.log(`  ✅ Loaded Key: ${key}`);
        }
    });

    // 2. Check Database Schema
    console.log('\n[2/3] Database Sync Status:');
    try {
        const userCols = await db.query("SELECT column_name FROM information_schema.columns WHERE table_name = 'users'");
        const cols = userCols.rows.map(r => r.column_name);

        if (cols.includes('role')) {
            console.log('  ✅ Users table has "role" column.');
        } else {
            console.error('  ❌ Users table MISSING "role" column. Run migrate:up!');
        }

        const tables = await db.query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
        const tableNames = tables.rows.map(r => r.table_name);
        ['wallets', 'notification_logs', 'fcm_tokens', 'saved_addresses'].forEach(t => {
            if (tableNames.includes(t)) {
                console.log(`  ✅ Table "${t}" exists.`);
            } else {
                console.error(`  ❌ MISSING Table: "${t}". Run migrate:up!`);
            }
        });

    } catch (e) {
        console.error('  ❌ Database Connection Failed:', e.message);
    }

    // 3. Check File Permissions
    console.log('\n[3/3] Storage verification:');
    const fs = require('fs');
    const uploadDir = path.join(__dirname, '../uploads');
    if (fs.existsSync(uploadDir)) {
        console.log('  ✅ Uploads directory exists.');
        try {
            const testFile = path.join(uploadDir, '.write_test');
            fs.writeFileSync(testFile, 'test');
            fs.unlinkSync(testFile);
            console.log('  ✅ Uploads directory is WRITABLE.');
        } catch (e) {
            console.error('  ❌ Uploads directory is NOT writable. Run: chmod -R 775 uploads');
        }
    } else {
        console.error('  ❌ Uploads directory MISSING. Run: mkdir uploads');
    }

    console.log('\nDiagnostic Complete. If everything is green and you still see 502, run: pm2 restart pikop-api\n');
    process.exit(0);
};

checkSystem();
