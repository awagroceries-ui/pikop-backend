const db = require('../src/config/db');

async function check() {
  try {
    const email = 'dr-keller@hotmail.com';
    const userRes = await db.query("SELECT id, full_name, email, role FROM users WHERE email ILIKE $1", [email]);
    console.log("USER:", userRes.rows);

    if (userRes.rows.length === 0) {
      console.log("User not found by email");
      return;
    }

    const userId = userRes.rows[0].id;

    const fulfillerRes = await db.query("SELECT id, user_id, full_name, email, primary_class, status FROM fulfillers WHERE user_id = $1 OR email ILIKE $2", [userId, email]);
    console.log("FULFILLERS:", fulfillerRes.rows);

    const ordersRes = await db.query(
      `SELECT id, status, user_id, fulfiller_id, queued_for_fulfiller_id, pickup_address, delivery_address, total_fare, created_at
       FROM orders
       WHERE fulfiller_id = $1 OR queued_for_fulfiller_id = $1 OR user_id = $1 OR fulfiller_id IN (SELECT id FROM fulfillers WHERE user_id = $1 OR email ILIKE $2) OR queued_for_fulfiller_id IN (SELECT id FROM fulfillers WHERE user_id = $1 OR email ILIKE $2)
       ORDER BY id DESC LIMIT 20`,
      [userId, email]
    );
    console.log("ORDERS FOR KELLER:", ordersRes.rows);

  } catch (e) {
    console.error("ERROR:", e);
  } finally {
    process.exit(0);
  }
}

check();
