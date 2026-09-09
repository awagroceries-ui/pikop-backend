const db = require('../config/db');

/**
 * Returns the current user's core profile.
 */
const getProfile = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(
            `SELECT u.id, u.full_name, u.email, u.phone, u.role, u.profile_photo_url, u.created_at,
                    f.kyc_status, f.bank_name, f.account_number, f.bank_code, f.account_name
             FROM users u
             LEFT JOIN fulfillers f ON f.user_id = u.id
             WHERE u.id = $1`,
            [userId]
        );
        if (rows.length === 0) return res.status(404).json({ success: false, message: 'User not found' });
        res.status(200).json(rows[0]);
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Updates user settings/profile info.
 */
const updateProfile = async (req, res) => {
    const userId = req.user.id;
    const {
        full_name, phone,
        bank_name, account_number, bank_code, account_name
    } = req.body;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Update Core User info
        await client.query(
            "UPDATE users SET full_name = COALESCE($1, full_name), phone = COALESCE($2, phone) WHERE id = $3",
            [full_name, phone, userId]
        );

        // 2. Update Fulfiller bank info if provided
        if (bank_name || account_number || bank_code || account_name) {
            await client.query(
                `UPDATE fulfillers
                 SET bank_name = COALESCE($1, bank_name),
                     account_number = COALESCE($2, account_number),
                     bank_code = COALESCE($3, bank_code),
                     account_name = COALESCE($4, account_name)
                 WHERE user_id = $5`,
                [bank_name, account_number, bank_code, account_name, userId]
            );
        }

        await client.query('COMMIT');
        res.status(200).json({ success: true, message: 'Profile updated' });
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

module.exports = {
    getProfile,
    updateProfile
};
