const db = require('../src/config/db');
require('dotenv').config();

async function fixSchema() {
    console.log('🛠️  FORCING DATABASE SCHEMA REPAIR...');

    try {
        const columns = [
            'mobility_type VARCHAR(50)',
            'registration_number VARCHAR(50)',
            'make VARCHAR(50)',
            'model VARCHAR(50)',
            'color VARCHAR(30)',
            'kyc_verified_at TIMESTAMP',
            'kyc_provider_ref VARCHAR(255)'
        ];

        for (const col of columns) {
            const [name] = col.split(' ');
            console.log(`[Schema] Checking column: ${name}...`);
            try {
                await db.query(`ALTER TABLE fulfillers ADD COLUMN ${col}`);
                console.log(`✅ Column ${name} added.`);
            } catch (e) {
                if (e.code === '42701') {
                    console.log(`ℹ️  Column ${name} already exists. Skipping.`);
                } else {
                    console.warn(`⚠️  Error adding ${name}:`, e.message);
                }
            }
        }

        console.log('✅ DATABASE SCHEMA REPAIR COMPLETE.');
        process.exit(0);
    } catch (error) {
        console.error('❌ FATAL REPAIR ERROR:', error.message);
        process.exit(1);
    }
}

fixSchema();
