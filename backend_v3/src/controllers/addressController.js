const db = require('../config/db');

/**
 * Retrieves user's saved addresses.
 */
const getSavedAddresses = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(
            "SELECT * FROM saved_addresses WHERE user_id = $1 ORDER BY last_used_at DESC",
            [userId]
        );
        res.status(200).json({ success: true, addresses: rows });
    } catch (error) {
        throw error;
    }
};

/**
 * Saves a new address or updates existing one.
 */
const saveAddress = async (req, res) => {
    const userId = req.user.id;
    const { label, address_text, lat, lng, landmark, place_id } = req.body;

    try {
        const { rows } = await db.query(
            `INSERT INTO saved_addresses (user_id, label, address_text, location, landmark, place_id)
             VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), $6, $7)
             ON CONFLICT (user_id, label) DO UPDATE SET
                address_text = EXCLUDED.address_text,
                location = EXCLUDED.location,
                landmark = EXCLUDED.landmark,
                place_id = EXCLUDED.place_id,
                last_used_at = CURRENT_TIMESTAMP
             RETURNING *`,
            [userId, label, address_text, lng, lat, landmark, place_id]
        );
        res.status(201).json({ success: true, address: rows[0] });
    } catch (error) {
        throw error;
    }
};

/**
 * Deletes a saved address.
 */
const deleteAddress = async (req, res) => {
    const userId = req.user.id;
    const { id } = req.params;
    try {
        await db.query("DELETE FROM saved_addresses WHERE id = $1 AND user_id = $2", [id, userId]);
        res.status(200).json({ success: true, message: 'Address deleted' });
    } catch (error) {
        throw error;
    }
};

module.exports = {
    getSavedAddresses,
    saveAddress,
    deleteAddress
};
