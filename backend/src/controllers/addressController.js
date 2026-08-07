const db = require('../config/db');

/**
 * Lists all saved addresses for the current user.
 */
const getSavedAddresses = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
      "SELECT id, label, address_text, ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng FROM saved_addresses WHERE user_id = $1 ORDER BY created_at DESC",
      [userId]
    );
    res.status(200).json(rows);
  } catch (error) {
    console.error('Get Addresses Error:', error);
    res.status(500).json({ error: 'Failed to fetch saved addresses' });
  }
};

/**
 * Saves a new address.
 */
const saveAddress = async (req, res) => {
  const userId = req.user.id;
  const { label, address_text, lat, lng } = req.body;

  try {
    const { rows } = await db.query(
      `INSERT INTO saved_addresses (user_id, label, address_text, location)
       VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326))
       RETURNING id, label, address_text`,
      [userId, label, address_text, lng, lat]
    );
    res.status(201).json(rows[0]);
  } catch (error) {
    console.error('Save Address Error:', error);
    res.status(500).json({ error: 'Failed to save address' });
  }
};

/**
 * Deletes a saved address.
 */
const deleteAddress = async (req, res) => {
  const userId = req.user.id;
  const { id } = req.params;

  try {
    await db.query(
      "DELETE FROM saved_addresses WHERE id = $1 AND user_id = $2",
      [id, userId]
    );
    res.status(200).json({ message: 'Address deleted' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to delete address' });
  }
};

module.exports = {
  getSavedAddresses,
  saveAddress,
  deleteAddress
};
