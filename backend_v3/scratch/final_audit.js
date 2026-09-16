const db = require('../src/config/db');

async function audit() {
  try {
    console.log('--- System Integrity Audit ---');

    // 1. Check Orders table schema for new columns
    const columnsRes = await db.query(`
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_name = 'orders'
        AND column_name IN ('scheduled_at', 'dispatch_commission_amount', 'merchant_commission_amount')
    `);
    console.log('Order Table Columns:', columnsRes.rows);

    // 2. Check Settings for unified fees
    const settingsRes = await db.query("SELECT * FROM settings WHERE key IN ('food_commission', 'groceries_commission', 'shop_commission', 'guest_sms_charge', 'platform_commission', 'cod_fee_rate')");
    console.log('Platform Settings:', settingsRes.rows);

    // 3. Check for any 'pending' (lowercase) merchants that bypassed verification
    const bypassVendors = await db.query("SELECT count(*) FROM vendors WHERE status = 'pending'");
    const bypassKitchens = await db.query("SELECT count(*) FROM kitchens WHERE status = 'pending'");
    console.log(`Bypass Merchants Found: Vendors=${bypassVendors.rows[0].count}, Kitchens=${bypassKitchens.rows[0].count}`);

    // 4. Check for unhandled Scheduled missions
    const scheduledCount = await db.query("SELECT count(*) FROM orders WHERE status = 'SCHEDULED'");
    console.log(`Scheduled Missions Awaiting Activation: ${scheduledCount.rows[0].count}`);

    process.exit(0);
  } catch (e) {
    console.error('Audit failed:', e.message);
    process.exit(1);
  }
}

audit();
