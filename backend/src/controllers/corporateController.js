const db = require('../config/db');

/**
 * Initializes a new Corporate Account.
 */
const createAccount = async (req, res) => {
  const { company_name, billing_email, billing_type } = req.body;
  const userId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Create Corporate Account
    const accountRes = await client.query(
      `INSERT INTO corporate_accounts (company_name, billing_email, billing_type, status)
       VALUES ($1, $2, $3, 'pending') RETURNING id, company_name`,
      [company_name, billing_email, billing_type]
    );
    const accountId = accountRes.rows[0].id;

    // 2. Create first sub-account (Billing Admin)
    await client.query(
      `INSERT INTO corporate_sub_accounts (corporate_account_id, user_id, role)
       VALUES ($1, $2, 'billing_admin')`,
      [accountId, userId]
    );

    // 3. Create Corporate Wallet if prepaid
    if (billing_type === 'prepaid_wallet') {
        await client.query(
            "INSERT INTO wallets (corporate_account_id, owner_type, balance) VALUES ($1, 'CORPORATE', 0)",
            [accountId]
        );
    }

    await client.query('COMMIT');
    res.status(201).json({
      message: 'Corporate account created. Please authorize payment mandate if applicable.',
      account: accountRes.rows[0]
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Create Corporate Account Error:', error);
    res.status(500).json({ error: 'Failed to create corporate account' });
  } finally {
    client.release();
  }
};

/**
 * Initiates a Paystack Direct Debit mandate.
 */
const authorizeMandate = async (req, res) => {
    const { id } = req.params;
    // For alpha: Return a mock URL that would trigger a webhook on completion
    res.status(200).json({
        authorization_url: `https://checkout.paystack.com/mandate_mock_${id}`,
        message: 'Direct Debit mandate flow initiated.'
    });
};

/**
 * Adds staff to a corporate account.
 */
const addStaff = async (req, res) => {
  const { id } = req.params;
  const { email, role } = req.body;
  const adminUserId = req.user.id;

  try {
    // 1. Verify admin permissions
    const checkRes = await db.query(
      "SELECT role FROM corporate_sub_accounts WHERE corporate_account_id = $1 AND user_id = $2",
      [id, adminUserId]
    );

    if (checkRes.rows.length === 0 || checkRes.rows[0].role !== 'billing_admin') {
      return res.status(403).json({ error: 'Unauthorized: Only billing_admins can add staff' });
    }

    // 2. Find user by email
    const userRes = await db.query("SELECT id FROM users WHERE email = $1", [email]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ error: 'User with this email not found. They must sign up for Pikop first.' });
    }
    const staffUserId = userRes.rows[0].id;

    // 3. Create sub-account
    await db.query(
      `INSERT INTO corporate_sub_accounts (corporate_account_id, user_id, role)
       VALUES ($1, $2, $3) ON CONFLICT DO NOTHING`,
      [id, staffUserId, role || 'staff']
    );

    res.status(201).json({ message: 'Staff added successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to add staff' });
  }
};

/**
 * Fetches staff for an account.
 */
const getStaff = async (req, res) => {
    const { id } = req.params;
    try {
        const { rows } = await db.query(`
            SELECT u.full_name, u.email, csa.role, csa.created_at
            FROM corporate_sub_accounts csa
            JOIN users u ON u.id = csa.user_id
            WHERE csa.corporate_account_id = $1`, [id]);
        res.status(200).json(rows);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch staff' });
    }
};

/**
 * Lists corporate accounts the user belongs to.
 */
const getMyAccounts = async (req, res) => {
    const userId = req.user.id;
    try {
        // Robust query: Try with status active, but if it fails, fallback to simple join
        const { rows } = await db.query(`
            SELECT ca.id, ca.company_name, ca.billing_type, csa.role
            FROM corporate_accounts ca
            JOIN corporate_sub_accounts csa ON csa.corporate_account_id = ca.id
            WHERE csa.user_id = $1`, [userId]);
        res.status(200).json(rows);
    } catch (error) {
        console.error('[Corporate] getMyAccounts Error:', error.message);
        res.status(500).json({ error: 'Failed to fetch your corporate accounts' });
    }
};

module.exports = {
  createAccount,
  authorizeMandate,
  addStaff,
  getStaff,
  getMyAccounts
};
