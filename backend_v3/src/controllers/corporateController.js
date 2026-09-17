const db = require('../config/db');
const walletService = require('../services/walletService');

/**
 * Stage 2 Onboarding: Corporate Business Verification.
 */
const setupCorporateProfile = async (req, res) => {
    const userId = req.user.id;
    const { company_name, cac_number, business_address, billing_email, billing_type } = req.body;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Create/Update Corporate Account
        const { rows } = await client.query(
            `INSERT INTO corporate_accounts (owner_user_id, company_name, cac_number, business_address, billing_email, billing_type)
             VALUES ($1, $2, $3, $4, $5, $6)
             ON CONFLICT (owner_user_id) DO UPDATE SET
                company_name = EXCLUDED.company_name,
                cac_number = EXCLUDED.cac_number,
                business_address = EXCLUDED.business_address,
                billing_email = EXCLUDED.billing_email,
                billing_type = EXCLUDED.billing_type
             RETURNING id, status`,
            [userId, company_name, cac_number, business_address, billing_email, billing_type || 'prepaid_wallet']
        );
        const accountId = rows[0].id;

        // 2. Add Owner as Admin in Sub-Accounts
        await client.query(
            `INSERT INTO corporate_sub_accounts (corporate_account_id, user_id, role)
             VALUES ($1, $2, 'ADMIN') ON CONFLICT DO NOTHING`,
            [accountId, userId]
        );

        // 3. Ensure Corporate Wallet exists
        await walletService.ensureWalletExists(client, 'CORPORATE', accountId);

        // 4. Update User Role to CORPORATE
        await client.query("UPDATE users SET role = 'CORPORATE' WHERE id = $1", [userId]);

        await client.query('COMMIT');
        res.status(201).json({ success: true, message: 'Corporate profile submitted for verification.', account_id: accountId });
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

/**
 * Returns dashboard data for a Corporate Admin.
 */
const getCorporateDashboard = async (req, res) => {
    const userId = req.user.id;
    try {
        // 1. Resolve Corporate Account
        const { rows: accRes } = await db.query(
            "SELECT ca.* FROM corporate_accounts ca JOIN corporate_sub_accounts csa ON csa.corporate_account_id = ca.id WHERE csa.user_id = $1 AND csa.role = 'ADMIN' LIMIT 1",
            [userId]
        );
        if (accRes.length === 0) return res.status(404).json({ success: false, message: 'Corporate account not found or access denied.' });
        const account = accRes[0];

        // 2. Fetch Wallet Balance
        const { rows: wRes } = await db.query("SELECT balance, pending_balance FROM wallets WHERE owner_type = 'CORPORATE' AND owner_id = $1", [account.id]);
        const wallet = wRes[0] || { balance: 0, pending_balance: 0 };

        // 3. Aggregate Spend Stats (30 Days)
        const { rows: stats } = await db.query(`
            SELECT
                COUNT(*) as total_orders,
                COALESCE(SUM(total_fare), 0) as total_spend,
                COUNT(DISTINCT user_id) as active_staff
            FROM orders
            WHERE corporate_account_id = $1
            AND created_at >= NOW() - interval '30 days'
        `, [account.id]);

        // 4. Spend by User
        const { rows: userSpend } = await db.query(`
            SELECT u.full_name, COALESCE(SUM(o.total_fare), 0) as spend
            FROM orders o
            JOIN users u ON u.id = o.user_id
            WHERE o.corporate_account_id = $1
            GROUP BY u.full_name
            ORDER BY spend DESC LIMIT 5
        `, [account.id]);

        res.status(200).json({
            success: true,
            data: {
                account,
                wallet,
                stats: stats[0],
                top_users: userSpend
            }
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Authorizes a new user (staff) via email.
 */
const addStaffMember = async (req, res) => {
    const adminUserId = req.user.id;
    const { email, role = 'STAFF', daily_limit = 0, monthly_limit = 0 } = req.body;

    try {
        // 1. Verify Admin owns the account
        const { rows: accRes } = await db.query(
            "SELECT corporate_account_id FROM corporate_sub_accounts WHERE user_id = $1 AND role = 'ADMIN' LIMIT 1",
            [adminUserId]
        );
        if (accRes.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });
        const accountId = accRes[0].corporate_account_id;

        // 2. Find target user
        const { rows: uRes } = await db.query("SELECT id FROM users WHERE email = $1", [email.toLowerCase().trim()]);
        if (uRes.length === 0) return res.status(404).json({ success: false, message: 'User not found. They must register on Pikop first.' });
        const targetUserId = uRes[0].id;

        // 3. Link
        await db.query(
            `INSERT INTO corporate_sub_accounts (corporate_account_id, user_id, role, daily_spend_limit, monthly_spend_limit)
             VALUES ($1, $2, $3, $4, $5)
             ON CONFLICT (corporate_account_id, user_id) DO UPDATE SET
                role = EXCLUDED.role,
                daily_spend_limit = EXCLUDED.daily_spend_limit,
                monthly_spend_limit = EXCLUDED.monthly_spend_limit`,
            [accountId, targetUserId, role, daily_limit, monthly_limit]
        );

        res.status(201).json({ success: true, message: 'Staff member authorized.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns authorized staff for a corporate account.
 */
const getStaffMembers = async (req, res) => {
    const adminUserId = req.user.id;
    try {
        const { rows } = await db.query(`
            SELECT u.id, u.full_name, u.email, csa.role, csa.daily_spend_limit, csa.monthly_spend_limit, csa.created_at
            FROM corporate_sub_accounts csa
            JOIN users u ON u.id = csa.user_id
            WHERE csa.corporate_account_id = (SELECT corporate_account_id FROM corporate_sub_accounts WHERE user_id = $1 AND role = 'ADMIN' LIMIT 1)
            ORDER BY u.full_name ASC
        `, [adminUserId]);
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns accounts the user is authorized to bill to.
 */
const getMyAccounts = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(`
            SELECT ca.id, ca.company_name, ca.billing_type, csa.role, csa.daily_spend_limit, csa.monthly_spend_limit
            FROM corporate_accounts ca
            JOIN corporate_sub_accounts csa ON csa.corporate_account_id = ca.id
            WHERE csa.user_id = $1 AND ca.status = 'ACTIVE'
        `, [userId]);
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

module.exports = {
    setupCorporateProfile,
    getCorporateDashboard,
    addStaffMember,
    getStaffMembers,
    getMyAccounts
};
