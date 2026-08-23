const db = require('../src/config/db');
require('dotenv').config();

async function check() {
    console.log('--- ORDER ACTIVATION DIAGNOSTIC ---');
    try {
        const { rows: columns } = await db.query(`
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = 'orders'
        `);
        console.log('✅ Current Columns in "orders":');
        columns.forEach(c => console.log(`   - ${c.column_name} (${c.data_type})`));

        const { rows: missions } = await db.query("SELECT id, status, quote_id, payment_status FROM orders ORDER BY created_at DESC LIMIT 5");
        console.log('\n✅ Latest Missions:');
        missions.forEach(m => console.log(`   ID: ${m.id} | Status: ${m.status} | Quote: ${m.quote_id} | Paid: ${m.payment_status}`));

        const { rows: quotes } = await db.query("SELECT id, total_fare FROM quotes ORDER BY created_at DESC LIMIT 5");
        console.log('\n✅ Latest Quotes:');
        quotes.forEach(q => console.log(`   ID: ${q.id} | Fare: ${q.total_fare}`));

        process.exit(0);
    } catch (e) {
        console.error('❌ Error:', e.message);
        process.exit(1);
    }
}
check();
