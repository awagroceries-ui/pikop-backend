const db = require('../config/db');
const bcrypt = require('bcrypt');

/**
 * Handles admin login.
 */
const login = async (req, res) => {
  const { username, password } = req.body;
  try {
    const { rows } = await db.query('SELECT * FROM admin_users WHERE username = $1', [username]);
    if (rows.length === 0) return res.render('login', { error: 'Invalid credentials', layout: false });

    const admin = rows[0];
    const match = await bcrypt.compare(password, admin.password_hash);
    if (!match) return res.render('login', { error: 'Invalid credentials', layout: false });

    req.session.adminId = admin.id;
    req.session.adminRole = admin.role;
    req.session.adminUsername = admin.username;

    res.redirect('/admin/dashboard');
  } catch (error) {
    res.render('login', { error: 'Server error', layout: false });
  }
};

/**
 * Main dashboard stats with trend data.
 */
const getDashboard = async (req, res) => {
  try {
    const activeOrders = await db.query("SELECT COUNT(*) FROM orders WHERE status IN ('SEARCHING', 'MATCHED', 'PICKED_UP')");
    const onlineFulfillers = await db.query("SELECT COUNT(*) FROM fulfillers WHERE online_status = 'ONLINE'");

    const totalRevenueQuery = await db.query(`
      SELECT SUM(le.amount) as total
      FROM wallet_ledger_entries le
      JOIN wallets w ON le.wallet_id = w.id
      WHERE w.owner_type = 'PLATFORM' AND le.entry_type = 'CREDIT'
    `);

    // Fetch 7-day sparkline data for revenue
    const sparklineRevenue = await db.query(`
      SELECT DATE(le.created_at) as date, SUM(le.amount) as amount
      FROM wallet_ledger_entries le
      JOIN wallets w ON le.wallet_id = w.id
      WHERE w.owner_type = 'PLATFORM' AND le.entry_type = 'CREDIT'
      AND le.created_at >= CURRENT_DATE - INTERVAL '7 days'
      GROUP BY DATE(le.created_at)
      ORDER BY date ASC
    `);

    res.render('dashboard', {
      admin: req.session.adminUsername,
      role: req.session.adminRole,
      stats: {
        activeOrders: activeOrders.rows[0].count,
        onlineFulfillers: onlineFulfillers.rows[0].count,
        totalRevenue: totalRevenueQuery.rows[0].total || 0,
        revenueTrend: sparklineRevenue.rows.map(r => r.amount)
      }
    });
  } catch (error) {
    console.error('Dashboard Stats Error:', error);
    res.status(500).send('Error loading dashboard: ' + error.message);
  }
};

/**
 * Daily revenue report for Chart.js.
 */
const getRevenueReport = async (req, res) => {
  const days = req.query.days || 30;
  try {
    const { rows } = await db.query(`
      SELECT TO_CHAR(le.created_at, 'Mon DD') as label, SUM(le.amount) as value
      FROM wallet_ledger_entries le
      JOIN wallets w ON le.wallet_id = w.id
      WHERE w.owner_type = 'PLATFORM' AND le.entry_type = 'CREDIT'
      AND le.created_at >= CURRENT_DATE - INTERVAL '${days} days'
      GROUP BY DATE(le.created_at), label
      ORDER BY DATE(le.created_at) ASC
    `);
    res.json(rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

/**
 * Daily order volume report for Chart.js.
 */
const getOrderReport = async (req, res) => {
  const days = req.query.days || 30;
  try {
    const { rows } = await db.query(`
      SELECT TO_CHAR(created_at, 'Mon DD') as label,
             COUNT(*) FILTER (WHERE status = 'DELIVERED') as completed,
             COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled
      FROM orders
      WHERE created_at >= CURRENT_DATE - INTERVAL '${days} days'
      GROUP BY DATE(created_at), label
      ORDER BY DATE(created_at) ASC
    `);
    res.json(rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

/**
 * KYC approval queue.
 */
const getKYCQueue = async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT k.*, f.user_id, u.full_name
      FROM kyc_documents k
      JOIN fulfillers f ON f.id = k.fulfiller_id
      JOIN users u ON u.id = f.user_id
      WHERE k.status = 'PENDING'
    `);
    res.render('kyc_queue', { documents: rows, admin: req.session.adminUsername });
  } catch (error) {
    res.status(500).send('Error loading KYC queue');
  }
};

const approveKYC = async (req, res) => {
  const { docId } = req.params;
  try {
    await db.query("UPDATE kyc_documents SET status = 'APPROVED' WHERE id = $1", [docId]);
    const { rows } = await db.query("SELECT fulfiller_id FROM kyc_documents WHERE id = $1", [docId]);
    await db.query("UPDATE fulfillers SET kyc_status = 'VERIFIED' WHERE id = $1", [rows[0].fulfiller_id]);
    res.redirect('/admin/kyc');
  } catch (error) {
    res.status(500).send('Error approving KYC');
  }
};

/**
 * List all orders.
 */
const getOrders = async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT o.*, u.full_name as customer_name, f.id as fulfiller_id
      FROM orders o
      JOIN users u ON u.id = o.user_id
      LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
      ORDER BY o.created_at DESC
    `);
    res.render('orders', { orders: rows, admin: req.session.adminUsername });
  } catch (error) {
    res.status(500).send('Error loading orders');
  }
};

/**
 * List all withdrawals.
 */
const getWithdrawals = async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT w.*, u.full_name
      FROM withdrawals w
      JOIN fulfillers f ON f.id = w.fulfiller_id
      JOIN users u ON u.id = f.user_id
      ORDER BY w.created_at DESC
    `);
    res.render('withdrawals', { withdrawals: rows, admin: req.session.adminUsername });
  } catch (error) {
    res.status(500).send('Error loading withdrawals');
  }
};

/**
 * Manage platform settings.
 */
const getSettings = async (req, res) => {
  try {
    const { rows } = await db.query("SELECT key, value FROM settings");
    const settingsMap = {};
    rows.forEach(r => settingsMap[r.key] = r.value);

    res.render('settings', {
      settings: settingsMap,
      admin: req.session.adminUsername,
      process: process
    });
  } catch (error) {
    res.status(500).send('Error loading settings');
  }
};

const updateSettings = async (req, res) => {
  const { platform_commission } = req.body;
  try {
    await db.query(
      "UPDATE settings SET value = $1, updated_at = CURRENT_TIMESTAMP WHERE key = 'platform_commission'",
      [platform_commission]
    );
    res.redirect('/admin/settings');
  } catch (error) {
    res.status(500).send('Error updating settings');
  }
};

module.exports = {
  login,
  getDashboard,
  getKYCQueue,
  approveKYC,
  getOrders,
  getWithdrawals,
  getSettings,
  updateSettings,
  getRevenueReport,
  getOrderReport
};
