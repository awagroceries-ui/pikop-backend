const db = require('../config/db');

/**
 * Returns the current user's core profile.
 */
const getProfile = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(
            "SELECT id, full_name, email, phone, role, profile_photo_url, created_at FROM users WHERE id = $1",
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
    const { full_name, phone } = req.body;
    try {
        await db.query(
            "UPDATE users SET full_name = COALESCE($1, full_name), phone = COALESCE($2, phone) WHERE id = $3",
            [full_name, phone, userId]
        );
        res.status(200).json({ success: true, message: 'Profile updated' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

module.exports = {
    getProfile,
    updateProfile
};
