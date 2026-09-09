const db = require('../config/db');
const bcrypt = require('bcryptjs');
const emailService = require('../services/emailService');

const walletService = require('../services/walletService');
const paystackService = require('../services/paystackService');

/**
 * Handles admin login.
 */
const login = async (req, res) => {
  const { username, password } = req.body;
  try {
    const { rows } = await db.query('SELECT * FROM admin_users WHERE username = $1', [username]);

    if (rows.length === 0) {
        console.warn(`[Admin] Login Fail: User '${username}' not found in DB.`);
        return res.render('login', { error: 'Access Denied: User not found', layout: false });
    }

    const admin = rows[0];
    const match = await bcrypt.compare(password, admin.password_hash);

    console.log(`[Admin] Login attempt: user=${username} | match=${match} | hash_preview=${admin.password_hash.substring(0, 10)}...`);

    if (!match) return res.render('login', { error: 'Invalid credentials', layout: false });

    req.session.adminId = admin.id;
    req.session.adminRole = admin.role;
    req.session.adminUsername = admin.username;

    res.redirect('/admin/dashboard');
  } catch (error) {
    console.error('[Admin] Login Error:', error.message);
    res.render('login', { error: `Server error: ${error.message}`, layout: false });
  }
};

/**
 * Safe First Signup: Only works if no admins exist.
 */
const getSignup = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT COUNT(*) FROM admin_users");
        if (parseInt(rows[0].count) > 0) {
            return res.redirect('/admin/login');
        }
        res.render('signup', { layout: false });
    } catch (error) {
        res.status(500).send('Setup Check Failed');
    }
};

const postSignup = async (req, res) => {
    const { username, password } = req.body;
    try {
        const { rows } = await db.query("SELECT COUNT(*) FROM admin_users");
        if (parseInt(rows[0].count) > 0) {
            return res.status(403).send('System already initialized');
        }

        const passwordHash = await bcrypt.hash(password, 10);
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, 'super_admin')",
            [username, passwordHash]
        );

        res.redirect('/admin/login');
    } catch (error) {
        res.status(500).send(`Signup Failed: ${error.message}`);
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

    // Total Users Breakdown
    const totalUsersRes = await db.query("SELECT COUNT(*) FROM users");
    const customerUsersRes = await db.query("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'");
    const fulfillerUsersRes = await db.query("SELECT COUNT(*) FROM users WHERE role = 'FULFILLER'");

    // Alert counts
    const pendingKYC = await db.query("SELECT COUNT(*) FROM fulfillers WHERE kyc_status = 'PENDING_REVIEW'");
    const supportConv = await db.query("SELECT COUNT(*) FROM conversations WHERE status = 'OPEN'");

    res.render('dashboard', {
      stats: {
        activeOrders: activeOrdersRes.rows[0].count || 0,
        onlineFulfillers: onlineFulfillersRes.rows[0].count || 0,
        totalRevenue: parseFloat(revenueRes.rows[0].sum || 0),
        totalUsers: parseInt(totalUsersRes.rows[0].count || 0),
        customerUsers: parseInt(customerUsersRes.rows[0].count || 0),
        fulfillerUsers: parseInt(fulfillerUsersRes.rows[0].count || 0),
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
    const { type, status } = req.query;
    try {
        let query = `
            SELECT o.*, u.full_name as user_name, f.full_name as fulfiller_name
            FROM orders o
            JOIN users u ON u.id = o.user_id
            LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE 1=1
        `;
        const params = [];
        if (type === 'COD') {
            query += " AND o.item_price > 0";
        }
        if (status) {
            params.push(status);
            query += ` AND o.status = $${params.length}`;
        }

        query += " ORDER BY o.created_at DESC LIMIT 100";

        const { rows } = await db.query(query, params);
        res.render('orders', { orders: rows, filters: { type, status } });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Live Mission Tracking for Admin.
 */
const trackOrder = async (req, res) => {
    const { id } = req.params;
    try {
        const { rows } = await db.query(`
            SELECT o.*,
            ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
            ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng,
            u.full_name as customer_name, u.phone as customer_phone,
            f.full_name as agent_name, f.phone as agent_phone, f.kyc_status,
            ST_Y(f.current_location::geometry) as agent_lat, ST_X(f.current_location::geometry) as agent_lng
            FROM orders o
            JOIN users u ON u.id = o.user_id
            LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE o.id = $1`, [id]);

        if (rows.length === 0) return res.status(404).render('error', { message: 'Mission not found' });

        // Fetch Ledger for this order
        const ledger = await db.query(
            "SELECT l.*, w.owner_type FROM wallet_ledger_entries l JOIN wallets w ON w.id = l.wallet_id WHERE l.order_id = $1 ORDER BY l.created_at ASC",
            [id]
        );

        res.render('admin_track', { order: rows[0], ledger: ledger.rows });
    } catch (error) {
        res.status(500).render('error', { message: error.message });
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
            SELECT c.*, u.full_name as participant_name,
            (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND is_read = false AND sender_type != 'ADMIN') as unread_count
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

const getKYCReview = async (req, res) => {
    const { id } = req.params;
    try {
        const fRes = await db.query(`
            SELECT f.*, u.full_name, u.email, u.phone as user_phone
            FROM fulfillers f
            JOIN users u ON u.id = f.user_id
            WHERE f.id = $1`, [id]);

        if (fRes.rows.length === 0) return res.status(404).send('Fulfiller not found');

        const docs = await db.query("SELECT * FROM kyc_documents WHERE fulfiller_id = $1", [id]);

        res.render('kyc_review', {
            f: fRes.rows[0],
            docs: docs.rows
        });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

const updateKYCStatus = async (req, res) => {
    const { id } = req.params;
    const { status, note } = req.body; // status: VERIFIED, REJECTED
    try {
        const newStatus = status === 'VERIFIED' ? 'active' : 'suspended';

        // Use explicit parameter indices to avoid type deduction ambiguity in Postgres
        await db.query(
            "UPDATE fulfillers SET kyc_status = $1, status = $2, approved_at = CASE WHEN $3 = 'VERIFIED' THEN CURRENT_TIMESTAMP ELSE approved_at END WHERE id = $4",
            [status, newStatus, status, id]
        );

        // Audit log
        await db.query(
            "INSERT INTO audit_logs (admin_id, action, target_type, target_id, payload) VALUES ($1, $2, $3, $4, $5)",
            [req.session.adminId, 'UPDATE_KYC', 'fulfiller', id, JSON.stringify({ status, note })]
        );

        // Fetch user email to send KYC status notification
        try {
            const { rows: fUser } = await db.query(
                "SELECT u.email, u.full_name FROM fulfillers f JOIN users u ON u.id = f.user_id WHERE f.id = $1",
                [id]
            );
            if (fUser.length > 0) {
                emailService.sendKycStatusEmail(fUser[0].email, fUser[0].full_name, status, note)
                    .catch(e => console.error('[KycEmail] Error:', e.message));
            }
        } catch (e) {}

        res.redirect('/admin/kyc');
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

const addAdmin = async (req, res) => {
    const { username, password, role } = req.body;
    try {
        const passwordHash = await bcrypt.hash(password, 10);
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, $3)",
            [username, passwordHash, role]
        );
        res.redirect('/admin/users');
    } catch (error) {
        res.status(500).send(`Failed to add admin: ${error.message}`);
    }
};

const deleteAdmin = async (req, res) => {
    const { id } = req.params;
    try {
        if (parseInt(id) === req.session.adminId) {
            return res.status(400).send('Self-deletion prohibited');
        }
        await db.query("DELETE FROM admin_users WHERE id = $1", [id]);
        res.redirect('/admin/users');
    } catch (error) {
        res.status(500).send('Deletion failed');
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

/**
 * Coupon Management (v3 Growth Engine)
 */
const getCoupons = async (req, res) => {
    try {
        const { rows } = await db.query("SELECT * FROM coupons ORDER BY created_at DESC");
        res.render('coupons_admin', { coupons: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

const createCoupon = async (req, res) => {
    const { code, discount_type, discount_value, min_order_amount, usage_limit } = req.body;
    try {
        await db.query(
            `INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, usage_limit, is_active)
             VALUES ($1, $2, $3, $4, $5, true)`,
            [code.toUpperCase(), discount_type, discount_value, min_order_amount || 0, usage_limit || 100]
        );
        res.redirect('/admin/coupons');
    } catch (error) {
        res.status(500).send(`Failed to create coupon: ${error.message}`);
    }
};

const deleteCoupon = async (req, res) => {
    const { id } = req.params;
    try {
        await db.query("DELETE FROM coupons WHERE id = $1", [id]);
        res.redirect('/admin/coupons');
    } catch (error) {
        res.status(500).send('Deletion failed');
    }
};

/**
 * Lists all active disputes.
 */
const getDisputes = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT d.*, o.item_description, o.item_price, o.total_fare,
                   u.full_name as reporter_name, f.full_name as fulfiller_name
            FROM disputes d
            JOIN orders o ON o.id = d.order_id
            JOIN users u ON u.id = d.reporter_id
            LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE d.status IN ('OPEN', 'INVESTIGATING')
            ORDER BY d.created_at DESC
        `);
        res.render('disputes', { disputes: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Resolves a dispute with either 'REFUND' or 'RELEASE'.
 */
const resolveDispute = async (req, res) => {
    const { id } = req.params;
    const { action, resolution_notes } = req.body;
    const adminId = req.session.adminId;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        const { rows } = await client.query("SELECT * FROM disputes WHERE id = $1 FOR UPDATE", [id]);
        if (rows.length === 0) throw new Error('Dispute not found');
        const dispute = rows[0];

        if (action === 'REFUND') {
            await walletService.refundEscrow(dispute.order_id);
        } else if (action === 'RELEASE') {
            await walletService.releaseEscrow(dispute.order_id);
        }

        await client.query(
            "UPDATE disputes SET status = 'RESOLVED', resolution_notes = $1 WHERE id = $2",
            [resolution_notes, id]
        );

        // Audit Log
        await client.query(
            "INSERT INTO audit_logs (admin_id, action, target_type, target_id, payload) VALUES ($1, $2, $3, $4, $5)",
            [adminId, `RESOLVE_DISPUTE_${action}`, 'dispute', id, JSON.stringify({ resolution_notes })]
        );

        await client.query('COMMIT');
        res.redirect('/admin/disputes');
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).send(`Resolution failed: ${error.message}`);
    } finally {
        client.release();
    }
};

/**
 * Lists all pending withdrawals.
 */
const getWithdrawals = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT w.*, f.full_name as fulfiller_name, f.bank_name, f.account_number, f.paystack_recipient_code
            FROM withdrawals w
            JOIN fulfillers f ON f.id = w.fulfiller_id
            WHERE w.status = 'PENDING'
            ORDER BY w.requested_at DESC
        `);
        res.render('withdrawals', { withdrawals: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Approves a withdrawal and initiates Paystack Transfer.
 */
const approveWithdrawal = async (req, res) => {
    const { id } = req.params;
    const adminId = req.session.adminId;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Lock withdrawal record
        const { rows } = await client.query(
            `SELECT w.*, f.full_name, f.account_number, f.bank_code, f.paystack_recipient_code, f.account_name
             FROM withdrawals w
             JOIN fulfillers f ON f.id = w.fulfiller_id
             WHERE w.id = $1 FOR UPDATE`,
            [id]
        );

        if (rows.length === 0) throw new Error('Withdrawal not found');
        const w = rows[0];

        if (w.status !== 'PENDING') throw new Error('Withdrawal already processed');

        // 2. Ensure Paystack Recipient exists
        let recipientCode = w.paystack_recipient_code;
        if (!recipientCode) {
            if (!w.account_number || !w.bank_code) throw new Error('Fulfiller bank details missing');

            // Use Account Name from DB if available, fallback to full_name
            const recipientName = w.account_name || w.full_name;
            const recipientRes = await paystackService.createTransferRecipient(recipientName, w.account_number, w.bank_code);
            recipientCode = recipientRes.data.recipient_code;

            await client.query("UPDATE fulfillers SET paystack_recipient_code = $1 WHERE id = $2", [recipientCode, w.fulfiller_id]);
        }

        // 3. Initiate Transfer
        const transferRef = `WDL_${id}_${Date.now()}`;
        const transferRes = await paystackService.initiateTransfer(w.amount, recipientCode, transferRef);

        // 4. Update Status to PROCESSING
        await client.query(
            "UPDATE withdrawals SET status = 'PROCESSING', paystack_transfer_code = $1 WHERE id = $2",
            [transferRes.data.transfer_code, id]
        );

        // Audit Log
        await client.query(
            "INSERT INTO audit_logs (admin_id, action, target_type, target_id, payload) VALUES ($1, $2, $3, $4, $5)",
            [adminId, 'APPROVE_WITHDRAWAL', 'withdrawal', id, JSON.stringify({ transfer_code: transferRes.data.transfer_code })]
        );

        await client.query('COMMIT');
        res.redirect('/admin/withdrawals');

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Admin] Withdrawal Approval Failed:', error.message);
        res.status(500).send(`Approval Error: ${error.message}`);
    } finally {
        client.release();
    }
};

/**
 * Fleet Management: List all fulfillers.
 */
const getFulfillers = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT f.*, u.full_name, u.email, u.phone as user_phone
            FROM fulfillers f
            JOIN users u ON u.id = f.user_id
            ORDER BY f.created_at DESC
        `);
        res.render('fulfillers', { fulfillers: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Fleet Management: Update fulfiller status (suspend, reactivate, terminate).
 */
const updateFulfillerStatus = async (req, res) => {
    const { id } = req.params;
    let { status } = req.body; // active, suspended, terminated

    // Safety: Handle array submission from UI (prevents constraint violation)
    if (Array.isArray(status)) status = status[0];

    try {
        await db.query("UPDATE fulfillers SET status = $1 WHERE id = $2", [status, id]);
        await db.query(
            "INSERT INTO audit_logs (admin_id, action, target_type, target_id, payload) VALUES ($1, $2, $3, $4, $5)",
            [req.session.adminId, 'UPDATE_FULFILLER_STATUS', 'fulfiller', id, JSON.stringify({ status })]
        );
        res.redirect('/admin/fulfillers');
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Manual Order Management: Update order status or force complete stuck orders.
 */
const updateOrderStatus = async (req, res) => {
    const { id } = req.params;
    let { status } = req.body; // e.g. DELIVERED, CANCELLED

    // Safety: Handle array submission from UI (prevents constraint violation)
    if (Array.isArray(status)) status = status[0];

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        const { rows } = await client.query("SELECT escrow_status FROM orders WHERE id = $1 FOR UPDATE", [id]);
        const order = rows[0];

        await client.query("UPDATE orders SET status = $1 WHERE id = $2", [status, id]);

        if (status === 'DELIVERED') {
            await client.query("UPDATE orders SET payment_status = 'PAID' WHERE id = $1", [id]);
            if (order && order.escrow_status === 'held') {
                try {
                    await walletService.releaseEscrow(id);
                } catch (e) {
                    console.error('[Admin] Escrow release on force complete failed:', e.message);
                }
            }
        }

        await client.query(
            "INSERT INTO audit_logs (admin_id, action, target_type, target_id, payload) VALUES ($1, $2, $3, $4, $5)",
            [req.session.adminId, 'MANUAL_ORDER_UPDATE', 'order', id, JSON.stringify({ status })]
        );

        await client.query('COMMIT');
        res.redirect(`/admin/orders/${id}/track`);
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).send(`Order update failed: ${error.message}`);
    } finally {
        client.release();
    }
};

/**
 * Transaction Audit Ledger: List all platform ledger entries and financial activity.
 */
const getTransactions = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT l.*, w.owner_type, w.owner_id
            FROM wallet_ledger_entries l
            JOIN wallets w ON w.id = l.wallet_id
            ORDER BY l.created_at DESC LIMIT 200
        `);
        res.render('transactions', { transactions: rows });
    } catch (error) {
        res.render('transactions', { transactions: [] });
    }
};

/**
 * Customer Management: List all customers.
 */
const getCustomers = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT u.*, w.balance, w.pending_balance
            FROM users u
            LEFT JOIN wallets w ON w.owner_type = 'USER' AND w.owner_id = u.id::text
            WHERE u.role = 'CUSTOMER'
            ORDER BY u.created_at DESC
        `);
        res.render('customers', { customers: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Customer Management: View individual customer details and activity.
 */
const getCustomerDetail = async (req, res) => {
    const { id } = req.params;
    try {
        const userRes = await db.query("SELECT * FROM users WHERE id = $1", [id]);
        if (userRes.rows.length === 0) return res.status(404).send('Customer not found');

        const ordersRes = await db.query("SELECT * FROM orders WHERE user_id = $1 ORDER BY created_at DESC LIMIT 50", [id]);
        const walletRes = await db.query("SELECT * FROM wallets WHERE owner_type = 'USER' AND owner_id = $1", [id.toString()]);

        let ledger = [];
        if (walletRes.rows.length > 0) {
            const ledgerRes = await db.query("SELECT * FROM wallet_ledger_entries WHERE wallet_id = $1 ORDER BY created_at DESC LIMIT 50", [walletRes.rows[0].id]);
            ledger = ledgerRes.rows;
        }

        res.render('customer_detail', {
            customer: userRes.rows[0],
            orders: ordersRes.rows,
            wallet: walletRes.rows[0] || { balance: 0, pending_balance: 0 },
            ledger
        });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Customer Management: Update customer status (suspend/activate).
 */
const updateCustomerStatus = async (req, res) => {
    const { id } = req.params;
    const { status } = req.body;
    try {
        await db.query("UPDATE users SET status = $1 WHERE id = $2", [status, id]).catch(() => {});
        res.redirect(`/admin/customers/${id}`);
    } catch (error) {
        res.status(500).send(error.message);
    }
};

const getFulfillerDetail = async (req, res) => {
    const { id } = req.params;
    try {
        const fRes = await db.query(`
            SELECT f.*, u.full_name as user_name, u.email as user_email, u.phone as user_phone
            FROM fulfillers f
            JOIN users u ON u.id = f.user_id
            WHERE f.id = $1`, [id]);

        if (fRes.rows.length === 0) return res.status(404).send('Fulfiller not found');

        const walletRes = await db.query("SELECT * FROM wallets WHERE owner_type = 'FULFILLER' AND owner_id = $1", [id.toString()]);

        let ledger = [];
        if (walletRes.rows.length > 0) {
            const ledgerRes = await db.query("SELECT * FROM wallet_ledger_entries WHERE wallet_id = $1 ORDER BY created_at DESC LIMIT 50", [walletRes.rows[0].id]);
            ledger = ledgerRes.rows;
        }

        res.render('fulfiller_detail', {
            fulfiller: fRes.rows[0],
            wallet: walletRes.rows[0] || { balance: 0, pending_balance: 0 },
            ledger
        });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

module.exports = {
  login,
  getSignup,
  postSignup,
  getDashboard,
  getOrders,
  trackOrder,
  getSettings,
  updateSettings,
  getSupportInbox,
  getConversationDetails,
  getKYCQueue,
  getKYCReview,
  updateKYCStatus,
  getVendors,
  getKitchens,
  getAdminUsers,
  addAdmin,
  deleteAdmin,
  getProfile,
  getCoupons,
  createCoupon,
  deleteCoupon,
  getDisputes,
  resolveDispute,
  getWithdrawals,
  approveWithdrawal,
  getFulfillers,
  getFulfillerDetail,
  updateFulfillerStatus,
  updateOrderStatus,
  getTransactions,
  getCustomers,
  getCustomerDetail,
  updateCustomerStatus
};
