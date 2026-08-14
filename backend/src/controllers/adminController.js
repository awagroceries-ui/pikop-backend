const db = require('../config/db');
const bcrypt = require('bcryptjs');
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

    // Fulfiller Class Performance (Agent, Rider, Driver)
    const classStats = await db.query(`
      SELECT
        f.primary_class,
        COUNT(o.id) as order_count,
        COALESCE(SUM(o.total_fare), 0) as revenue,
        COALESCE(AVG(o.rating), 5.0) as avg_rating
      FROM fulfillers f
      LEFT JOIN orders o ON o.fulfiller_id = f.id AND o.status = 'DELIVERED'
      GROUP BY f.primary_class
    `).catch(err => {
      console.warn('[Dashboard] Class Performance query fallback applied:', err.message);
      return { rows: [] }; // Fallback to empty if rating column still missing
    });

    // Notification Counts
    const pendingKYC = await db.query("SELECT COUNT(*) FROM fulfillers WHERE kyc_status = 'PENDING_REVIEW'").catch(() => ({ rows: [{count:0}] }));
    const openDisputes = await db.query("SELECT COUNT(*) FROM disputes WHERE status = 'OPEN'").catch(() => ({ rows: [{count:0}] }));
    const unreadSupport = await db.query("SELECT COUNT(*) FROM conversations WHERE status = 'OPEN'").catch(() => ({ rows: [{count:0}] }));

    // Fetch 7-day sparkline data for revenue
    const sparklineRevenue = await db.query(`
      SELECT DATE(le.created_at) as date, SUM(le.amount) as amount
      FROM wallet_ledger_entries le
      JOIN wallets w ON le.wallet_id = w.id
      WHERE w.owner_type = 'PLATFORM' AND le.entry_type = 'CREDIT'
      AND le.created_at >= CURRENT_DATE - INTERVAL '7 days'
      GROUP BY DATE(le.created_at)
      ORDER BY date ASC
    `).catch(() => ({ rows: [0,0,0,0,0,0,0] }));

    res.render('dashboard', {
      stats: {
        activeOrders: activeOrders.rows[0].count,
        onlineFulfillers: onlineFulfillers.rows[0].count,
        totalRevenue: totalRevenueQuery.rows[0].total || 0,
        revenueTrend: sparklineRevenue.rows.map(r => r.amount),
        classPerformance: classStats.rows,
        notifications: {
          kyc: pendingKYC.rows[0].count,
          disputes: openDisputes.rows[0].count,
          support: unreadSupport.rows[0].count
        }
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
      SELECT f.id as fulfiller_id, f.didit_verification_status, f.didit_session_id, f.primary_class, u.full_name, u.email
      FROM fulfillers f
      JOIN users u ON u.id = f.user_id
      WHERE f.kyc_status != 'VERIFIED' OR f.didit_verification_status != 'approved'
    `);

    // For each fulfiller, also get their manual documents
    for (let f of rows) {
      const docs = await db.query("SELECT * FROM kyc_documents WHERE fulfiller_id = $1", [f.fulfiller_id]);
      f.documents = docs.rows || []; // Ensure array is never undefined
    }

    res.render('kyc_queue', { fulfillers: rows });
  } catch (error) {
    console.error('KYC Queue Error:', error);
    res.status(500).send('Error loading KYC queue: ' + error.message);
  }
};

const forceApproveIdentity = async (req, res) => {
  const { id } = req.params;
  try {
    await db.query(
      "UPDATE fulfillers SET didit_verification_status = 'approved', didit_verified_at = CURRENT_TIMESTAMP WHERE id = $1",
      [id]
    );
    console.log(`[Admin] Force Approved Identity for Fulfiller ${id}`);
    res.redirect('/admin/kyc');
  } catch (error) {
    res.status(500).send('Error force approving identity');
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
      SELECT o.*, u.full_name as customer_name
      FROM orders o
      JOIN users u ON u.id = o.user_id
      LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
      ORDER BY o.created_at DESC
    `);
    res.render('orders', { orders: rows });
  } catch (error) {
    console.error('[Admin] getOrders Error:', error);
    res.status(500).render('error', { message: 'Error loading orders: ' + error.message });
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
      LEFT JOIN fulfillers f ON f.id = w.fulfiller_id
      LEFT JOIN users u ON u.id = f.user_id
      ORDER BY w.created_at DESC
    `);
    res.render('withdrawals', { withdrawals: rows });
  } catch (error) {
    console.error('[Admin] getWithdrawals Error:', error);
    res.status(500).render('error', { message: 'Error loading withdrawals: ' + error.message });
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
    console.error('[Admin] getDisputes Error:', error);
    res.status(500).render('error', { message: 'Error loading disputes: ' + error.message });
  }
};

/**
 * Support Inbox Logic
 * Handles both USER and FULFILLER participant types.
 */
const getSupportInbox = async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT
        c.*,
        CASE
          WHEN c.participant_type = 'USER' THEN u.full_name
          WHEN c.participant_type = 'FULFILLER' THEN fu.full_name
          ELSE 'Unknown Participant'
        END as participant_name
      FROM conversations c
      LEFT JOIN users u ON c.participant_type = 'USER' AND u.id = c.participant_id
      LEFT JOIN fulfillers f ON c.participant_type = 'FULFILLER' AND f.id = c.participant_id
      LEFT JOIN users fu ON f.user_id = fu.id
      WHERE c.status = 'OPEN'
      ORDER BY c.last_message_at DESC
    `);
    res.render('support_inbox', { conversations: rows });
  } catch (error) {
    console.error('[Admin] getSupportInbox Error:', error);
    res.status(500).render('error', { message: 'Error loading support inbox: ' + error.message });
  }
};

const getConversationDetails = async (req, res) => {
  const { id } = req.params;
  try {
    const convRes = await db.query(`
      SELECT
        c.*,
        CASE
          WHEN c.participant_type = 'USER' THEN u.full_name
          WHEN c.participant_type = 'FULFILLER' THEN fu.full_name
          ELSE 'Unknown Participant'
        END as full_name
      FROM conversations c
      LEFT JOIN users u ON c.participant_type = 'USER' AND u.id = c.participant_id
      LEFT JOIN fulfillers f ON c.participant_type = 'FULFILLER' AND f.id = c.participant_id
      LEFT JOIN users fu ON f.user_id = fu.id
      WHERE c.id = $1
    `, [id]);

    if (convRes.rows.length === 0) return res.status(404).render('error', { message: 'Conversation not found' });

    const msgRes = await db.query("SELECT * FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC", [id]);

    res.render('support_detail', {
      conversation: convRes.rows[0],
      messages: msgRes.rows,
      adminId: req.session.adminId
    });
  } catch (error) {
    console.error('[Admin] getConversationDetails Error:', error);
    res.status(500).render('error', { message: 'Error loading conversation: ' + error.message });
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
    if (parseInt(id) === req.session.adminId) {
      return res.status(400).send('You cannot delete your own account.');
    }
    await db.query("DELETE FROM admin_users WHERE id = $1", [id]);
    res.redirect('/admin/admins');
  } catch (error) {
    res.status(500).send('Error deleting admin user');
  }
};

/**
 * List all corporate accounts.
 */
const getCorporateAccounts = async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT ca.*, (SELECT COUNT(*) FROM corporate_sub_accounts WHERE corporate_account_id = ca.id) as staff_count
      FROM corporate_accounts ca
      ORDER BY ca.created_at DESC
    `);
    res.render('corporate_accounts', { accounts: rows });
  } catch (error) {
    console.error('[Admin] getCorporateAccounts Error:', error);
    res.status(500).render('error', { message: 'Error loading corporate accounts: ' + error.message });
  }
};

const suspendCorporateAccount = async (req, res) => {
  const { id } = req.params;
  try {
    await db.query("UPDATE corporate_accounts SET status = 'suspended' WHERE id = $1", [id]);
    res.redirect('/admin/corporate');
  } catch (error) {
    res.status(500).send('Error suspending account');
  }
};

/**
 * Provides comprehensive platform metrics (Prompt 4).
 */
const getOverviewMetrics = async (req, res) => {
  const days = parseInt(req.query.range) || 30;

  try {
    // 1. Registered Totals
    const totalUsers = await db.query("SELECT COUNT(*) FROM users");
    const totalFulfillers = await db.query("SELECT COUNT(*) FROM fulfillers");

    // 2. Active Users (DAU/MAU)
    const dauUsers = await db.query("SELECT COUNT(*) FROM users WHERE last_active_at >= NOW() - INTERVAL '24 hours'");
    const dauFulfillers = await db.query("SELECT COUNT(*) FROM fulfillers WHERE last_active_at >= NOW() - INTERVAL '24 hours'");
    const mauUsers = await db.query("SELECT COUNT(*) FROM users WHERE last_active_at >= NOW() - INTERVAL '30 days'");
    const mauFulfillers = await db.query("SELECT COUNT(*) FROM fulfillers WHERE last_active_at >= NOW() - INTERVAL '30 days'");

    // 3. Revenue Breakdown
    const revenueQuery = await db.query(`
      SELECT
        COALESCE(SUM(o.total_fare), 0) as gross,
        (SELECT COALESCE(SUM(le.amount), 0) FROM wallet_ledger_entries le JOIN wallets w ON le.wallet_id = w.id WHERE w.owner_type = 'PLATFORM' AND le.entry_type = 'CREDIT' AND le.created_at >= NOW() - INTERVAL '${days} days') as platform,
        (SELECT COALESCE(SUM(le.amount), 0) FROM wallet_ledger_entries le JOIN wallets w ON le.wallet_id = w.id WHERE w.owner_type = 'FULFILLER' AND le.entry_type = 'CREDIT' AND le.created_at >= NOW() - INTERVAL '${days} days') as fulfiller
      FROM orders o
      WHERE o.status IN ('DELIVERED', 'CLOSED')
      AND o.created_at >= NOW() - INTERVAL '${days} days'
    `);

    // 4. Class Performance (Agent, Rider, Driver)
    const classStats = await db.query(`
      SELECT
        f.primary_class,
        COUNT(o.id) as order_count,
        COALESCE(SUM(o.total_fare), 0) as revenue,
        COALESCE(AVG(o.rating), 5.0) as avg_rating
      FROM fulfillers f
      LEFT JOIN orders o ON o.fulfiller_id = f.id
        AND o.status = 'DELIVERED'
        AND o.created_at >= NOW() - INTERVAL '${days} days'
      GROUP BY f.primary_class
    `);

    res.status(200).json({
      registered: {
        total_users: totalUsers.rows[0].count,
        total_fulfillers: totalFulfillers.rows[0].count
      },
      active: {
        dau: { users: dauUsers.rows[0].count, fulfillers: dauFulfillers.rows[0].count },
        mau: { users: mauUsers.rows[0].count, fulfillers: mauFulfillers.rows[0].count }
      },
      revenue: revenueQuery.rows[0],
      classPerformance: classStats.rows,
      active_orders: (await db.query("SELECT COUNT(*) FROM orders WHERE status IN ('SEARCHING', 'MATCHED', 'PICKED_UP')")).rows[0].count
    });
  } catch (error) {
    console.error("Metrics Error:", error);
    res.status(500).json({ error: error.message });
  }
};

/**
 * Admin reply to support chat (Prompt 2).
 */
const replySupport = async (req, res) => {
  const { id } = req.params;
  const { body } = req.body;
  const adminId = req.session.adminId;

  try {
    const { rows } = await db.query(
      "INSERT INTO messages (conversation_id, sender_id, sender_type, body) VALUES ($1, $2, 'ADMIN', $3) RETURNING id, created_at",
      [id, adminId, body]
    );

    const socketService = require('../services/socketService');
    const io = socketService.getIO();
    io.to(`support_${id}`).emit("receive_message", {
        id: rows[0].id,
        conversationId: id,
        senderId: adminId,
        senderType: 'ADMIN',
        body: body,
        created_at: rows[0].created_at
    });

    // PUSH notification to participant
    const { rows: conv } = await db.query("SELECT participant_id FROM conversations WHERE id = $1", [id]);
    if (conv.length > 0) {
        const fcmService = require('../services/fcmService');
        await fcmService.sendNotification(conv[0].participant_id, "New Support Reply", body, { type: "SUPPORT_CHAT" });
    }

    res.redirect(`/admin/support/${id}`);
  } catch (error) {
    console.error("Support Reply Error:", error);
    res.status(500).send('Failed to send reply');
  }
};

/**
 * Approves a cancellation fee waiver (Prompt 6).
 */
const approveWaiver = async (req, res) => {
    const { id } = req.params; // Dispute ID
    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        const { rows } = await client.query("SELECT order_id FROM disputes WHERE id = $1", [id]);
        if (rows.length === 0) throw new Error('Dispute not found');
        const orderId = rows[0].order_id;

        // 1. Reverse Fee (Credit back is handled manually or via system credit, here we just flag)
        await client.query("UPDATE orders SET cancellation_fee_waived = true WHERE id = $1", [orderId]);

        // 2. Resolve Dispute
        await client.query("UPDATE disputes SET status = 'RESOLVED', resolution_notes = 'Waiver approved by admin' WHERE id = $1", [id]);

        await client.query('COMMIT');
        res.redirect('/admin/disputes');
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).send('Waiver approval failed');
    } finally {
        client.release();
    }
};

const denyWaiver = async (req, res) => {
    const { id } = req.params;
    try {
        await db.query("UPDATE disputes SET status = 'CLOSED', resolution_notes = 'Waiver denied by admin' WHERE id = $1", [id]);
        res.redirect('/admin/disputes');
    } catch (error) {
        res.status(500).send('Waiver denial failed');
    }
};

module.exports = {
  login,
  getDashboard,
  getOverviewMetrics,
  getKYCQueue,
  approveKYC,
  forceApproveIdentity,
  verifyFulfiller,
  getOrders,
  getWithdrawals,
  getDisputes,
  getSupportInbox,
  getConversationDetails,
  resolveSupport,
  replySupport,
  approveWaiver,
  denyWaiver,
  getCorporateAccounts,
  suspendCorporateAccount,
  getSettings,
  updateSettings,
  getRevenueReport,
  getOrderReport,
  getAdmins,
  addAdmin,
  deleteAdmin
};
