const db = require('../config/db');
const bcrypt = require('bcryptjs');

/**
 * Handles admin login.
 */
const login = async (req, res) => {
  const { username, password } = req.body;
  try {
    const { rows } = await db.query('SELECT * FROM admin_users WHERE username = $1', [username]);
    if (rows.length === 0) return res.render('login', { error: 'Access Denied: User not found', layout: false });

    const admin = rows[0];
    const match = await bcrypt.compare(password, admin.password_hash);
    if (!match) return res.render('login', { error: 'Access Denied: Invalid credentials', layout: false });

    req.session.adminId = admin.id;
    req.session.adminRole = admin.role;
    req.session.adminUsername = admin.username;

    res.redirect('/admin/dashboard');
  } catch (error) {
    res.render('login', { error: 'Server error', layout: false });
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

    // Alert counts
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

/**
 * List all orders with filters.
 */
const getOrders = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT o.*, u.full_name as user_name, f.full_name as fulfiller_name
            FROM orders o
            JOIN users u ON u.id = o.user_id
            LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
            ORDER BY o.created_at DESC LIMIT 100
        `);
        res.render('orders', { orders: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Platform Settings (Master Brief Milestone 23)
 */
const getSettings = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT key, value FROM settings");
        const settingsMap = {};
        rows.forEach(r => settingsMap[r.key] = r.value);
        res.render('settings', { settings: settingsMap });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

const updateSettings = async (req, res) => {
    const { base_fare_small, base_fare_medium, base_fare_large, per_km_rate, platform_commission } = req.body;
    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        const settings = [
            ['base_fare_small', base_fare_small],
            ['base_fare_medium', base_fare_medium],
            ['base_fare_large', base_fare_large],
            ['per_km_rate', per_km_rate],
            ['platform_commission', platform_commission]
        ];

        for (const [key, val] of settings) {
            if (val) {
                await client.query(
                    "INSERT INTO settings (key, value) VALUES ($1, $2) ON CONFLICT (key) DO UPDATE SET value = $2",
                    [key, val]
                );
            }
        }
        await client.query('COMMIT');
        res.redirect('/admin/settings');
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).send('Settings update failed');
    } finally {
        client.release();
    }
};

/**
 * Support Inbox (V3 Real-time Ready)
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
        res.status(500).send(error.message);
    }
};

const getConversationDetails = async (req, res) => {
    const { id } = req.params;
    try {
        const convRes = await db.query(`
            SELECT c.*, u.full_name as participant_name
            FROM conversations c
            JOIN users u ON u.id = c.participant_id
            WHERE c.id = $1`, [id]);

        if (convRes.rows.length === 0) return res.status(404).send('Not found');

        const messages = await db.query("SELECT * FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC", [id]);

        res.render('support_detail', {
            conversation: convRes.rows[0],
            messages: messages.rows,
            adminId: req.session.adminId
        });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * KYC Review Queue (v3)
 */
const getKYCQueue = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT f.*, u.full_name, u.email
            FROM fulfillers f
            JOIN users u ON u.id = f.user_id
            WHERE f.kyc_status != 'VERIFIED'
            ORDER BY f.created_at DESC
        `);
        res.render('kyc_queue', { fulfillers: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Business Entity Management
 */
const getVendors = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT * FROM vendors ORDER BY created_at DESC");
        res.render('vendors', { vendors: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

const getKitchens = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT * FROM kitchens ORDER BY created_at DESC");
        res.render('kitchens', { kitchens: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Lists all admin users.
 */
const getAdminUsers = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT id, username, role, created_at FROM admin_users ORDER BY role ASC");
        res.render('admin_users', { users: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Returns currently logged in admin profile.
 */
const getProfile = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT * FROM admin_users WHERE id = $1", [req.session.adminId]);
        res.render('admin_profile', { user: rows[0] });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

module.exports = {
  login,
  getDashboard,
  getOrders,
  getSettings,
  updateSettings,
  getSupportInbox,
  getConversationDetails,
  getKYCQueue,
  getVendors,
  getKitchens,
  getAdminUsers,
  getProfile
};
