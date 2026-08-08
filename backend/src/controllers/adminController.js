const db = require('../config/db');
const bcrypt = require('bcrypt');
const notificationService = require('../services/notificationService');

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
      SELECT f.id as fulfiller_id, f.didit_verification_status, f.didit_session_id, u.full_name, u.email
      FROM fulfillers f
      JOIN users u ON u.id = f.user_id
      WHERE f.kyc_status = 'PENDING'
    `);

    // For each fulfiller, also get their manual documents
    for (let f of rows) {
      const docs = await db.query("SELECT * FROM kyc_documents WHERE fulfiller_id = $1", [f.fulfiller_id]);
      f.documents = docs.rows;
    }

    res.render('kyc_queue', { fulfillers: rows });
  } catch (error) {
    res.status(500).send('Error loading KYC queue');
  }
};

const approveKYC = async (req, res) => {
  const { docId } = req.params;
  try {
    await db.query("UPDATE kyc_documents SET status = 'APPROVED' WHERE id = $1", [docId]);
    res.redirect('/admin/kyc');
  } catch (error) {
    res.status(500).send('Error approving document');
  }
};

const verifyFulfiller = async (req, res) => {
  const { id } = req.params;
  try {
    await db.query("UPDATE fulfillers SET kyc_status = 'VERIFIED' WHERE id = $1", [id]);

    // Get Fulfiller User ID for the email
    const { rows } = await db.query("SELECT user_id FROM fulfillers WHERE id = $1", [id]);
    notificationService.sendFulfillerApprovedEmail(id);

    res.redirect('/admin/kyc');
  } catch (error) {
    res.status(500).send('Error verifying fulfiller');
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
    res.render('orders', { orders: rows });
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
    res.render('withdrawals', { withdrawals: rows });
  } catch (error) {
    res.status(500).send('Error loading withdrawals');
  }
};

/**
 * List all disputes and incidents.
 */
const getDisputes = async (req, res) => {
  try {
    const { rows } = await db.query("SELECT * FROM disputes ORDER BY created_at DESC");
    res.render('disputes', { disputes: rows });
  } catch (error) {
    res.status(500).send('Error loading disputes');
  }
};

/**
 * Support Inbox Logic
 */
const getSupportInbox = async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT c.*, u.full_name as participant_name
      FROM conversations c
      JOIN users u ON u.id = c.participant_id
      WHERE c.status = 'OPEN'
      ORDER BY c.last_message_at DESC
    `);
    res.render('support_inbox', { conversations: rows });
  } catch (error) {
    res.status(500).send('Error loading support inbox');
  }
};

const getConversationDetails = async (req, res) => {
  const { id } = req.params;
  try {
    const convRes = await db.query("SELECT c.*, u.full_name FROM conversations c JOIN users u ON u.id = c.participant_id WHERE c.id = $1", [id]);
    if (convRes.rows.length === 0) return res.status(404).send('Conversation not found');

    const msgRes = await db.query("SELECT * FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC", [id]);

    res.render('support_detail', {
      conversation: convRes.rows[0],
      messages: msgRes.rows,
      adminId: req.session.adminId
    });
  } catch (error) {
    res.status(500).send('Error loading conversation details');
  }
};

const resolveSupport = async (req, res) => {
  const { id } = req.params;
  try {
    await db.query("UPDATE conversations SET status = 'CLOSED' WHERE id = $1", [id]);
    res.redirect('/admin/support');
  } catch (error) {
    res.status(500).send('Error closing conversation');
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

/**
 * Admin User Management
 */
const getAdmins = async (req, res) => {
  try {
    const { rows } = await db.query("SELECT id, username, role, created_at FROM admin_users ORDER BY created_at DESC");
    res.render('admins', { admins: rows });
  } catch (error) {
    res.status(500).send('Error loading admin users');
  }
};

const addAdmin = async (req, res) => {
  const { username, password, role } = req.body;
  try {
    const passwordHash = await bcrypt.hash(password, 10);
    await db.query(
      "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, $3)",
      [username, passwordHash, role]
    );
    res.redirect('/admin/admins');
  } catch (error) {
    console.error('Add Admin Error:', error);
    res.status(500).send('Error creating admin user');
  }
};

const deleteAdmin = async (req, res) => {
  const { id } = req.params;
  try {
    // Prevent deleting self
    if (parseInt(id) === req.session.adminId) {
      return res.status(400).send('You cannot delete your own account.');
    }
    await db.query("DELETE FROM admin_users WHERE id = $1", [id]);
    res.redirect('/admin/admins');
  } catch (error) {
    res.status(500).send('Error deleting admin user');
  }
};

module.exports = {
  login,
  getDashboard,
  getKYCQueue,
  approveKYC,
  verifyFulfiller,
  getOrders,
  getWithdrawals,
  getDisputes,
  getSupportInbox,
  getConversationDetails,
  resolveSupport,
  getSettings,
  updateSettings,
  getRevenueReport,
  getOrderReport,
  getAdmins,
  addAdmin,
  deleteAdmin
};
