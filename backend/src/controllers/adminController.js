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
 * Main dashboard stats.
 */
const getDashboard = async (req, res) => {
  try {
    const activeOrders = await db.query("SELECT COUNT(*) FROM orders WHERE status IN ('SEARCHING', 'MATCHED', 'PICKED_UP')");
    const onlineFulfillers = await db.query("SELECT COUNT(*) FROM fulfillers WHERE online_status = 'ONLINE'");
    const totalRevenue = await db.query("SELECT SUM(amount) FROM wallet_ledger_entries WHERE owner_type = 'PLATFORM' AND entry_type = 'CREDIT'");

    res.render('dashboard', {
      admin: req.session.adminUsername,
      role: req.session.adminRole,
      stats: {
        activeOrders: activeOrders.rows[0].count,
        onlineFulfillers: onlineFulfillers.rows[0].count,
        totalRevenue: totalRevenue.rows[0].sum || 0
      }
    });
  } catch (error) {
    res.status(500).send('Error loading dashboard');
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
    // Also update fulfiller status if all docs approved (simplified for alpha)
    const { rows } = await db.query("SELECT fulfiller_id FROM kyc_documents WHERE id = $1", [docId]);
    await db.query("UPDATE fulfillers SET kyc_status = 'VERIFIED' WHERE id = $1", [rows[0].fulfiller_id]);
    res.redirect('/admin/kyc');
  } catch (error) {
    res.status(500).send('Error approving KYC');
  }
};

module.exports = {
  login,
  getDashboard,
  getKYCQueue,
  approveKYC
};
