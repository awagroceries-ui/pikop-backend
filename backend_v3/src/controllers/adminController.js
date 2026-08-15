const db = require('../config/db');
const bcrypt = require('bcryptjs');

/**
 * Handles admin login.
 */
const login = async (req, res) => {
  const { username, password } = req.body;
  try {
    const { rows } = await db.query('SELECT * FROM admin_users WHERE username = $1', [username]);
    if (rows.length === 0) return res.status(401).send('Unauthorized');

    const admin = rows[0];
    const match = await bcrypt.compare(password, admin.password_hash);
    if (!match) return res.status(401).send('Unauthorized');

    req.session.adminId = admin.id;
    req.session.adminRole = admin.role;
    req.session.adminUsername = admin.username;

    res.redirect('/admin/dashboard');
  } catch (error) {
    res.status(500).send('Login error');
  }
};

/**
 * Renders the main dashboard with metrics.
 */
const getDashboard = async (req, res) => {
  try {
    const activeOrdersRes = await db.query("SELECT COUNT(*) FROM orders WHERE status NOT IN ('DELIVERED', 'CANCELLED')");
    const onlineFulfillersRes = await db.query("SELECT COUNT(*) FROM fulfillers WHERE online_status = 'ONLINE'");
    const revenueRes = await db.query("SELECT SUM(total_fare) FROM orders WHERE payment_status = 'PAID'");

    const pendingKYC = await db.query("SELECT COUNT(*) FROM fulfillers WHERE kyc_status = 'PENDING_REVIEW'");
    const supportConv = await db.query("SELECT COUNT(*) FROM conversations WHERE status = 'OPEN'");

    res.render('dashboard', {
      stats: {
        activeOrders: activeOrdersRes.rows[0].count || 0,
        onlineFulfillers: onlineFulfillersRes.rows[0].count || 0,
        totalRevenue: parseFloat(revenueRes.rows[0].sum || 0),
        notifications: {
          kyc: pendingKYC.rows[0].count || 0,
          support: supportConv.rows[0].count || 0,
          disputes: 0
        }
      }
    });
  } catch (error) {
    res.status(500).render('error', { message: error.message });
  }
};

module.exports = {
  login,
  getDashboard
};
