const db = require('../src/config/db');

async function check() {
  try {
    const colRes = await db.query("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'orders' AND column_name LIKE '%fulfiller%'");
    console.log("COLUMNS:", colRes.rows);

    const ordersRes = await db.query("SELECT id, status, user_id, fulfiller_id, queued_for_fulfiller_id, created_at FROM orders ORDER BY id DESC LIMIT 10");
    console.log("RECENT ORDERS:", ordersRes.rows);

    const fulfillersRes = await db.query("SELECT id, user_id, full_name, email FROM fulfillers LIMIT 10");
    console.log("FULFILLERS:", fulfillersRes.rows);
  } catch (e) {
    console.error(e);
  } finally {
    process.exit(0);
  }
}

check();
