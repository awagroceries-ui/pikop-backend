const db = require('../config/db');

/**
 * Updates basic profile information.
 */
const updateProfile = async (req, res) => {
  const userId = req.user.id;
  const { full_name, phone, language } = req.body;

  try {
    await db.query(
      "UPDATE users SET full_name = COALESCE($1, full_name), phone = COALESCE($2, phone), language = COALESCE($3, language), updated_at = CURRENT_TIMESTAMP WHERE id = $4",
      [full_name, phone, language, userId]
    );
    res.status(200).json({ message: 'Profile updated successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to update profile' });
  }
};

/**
 * Updates notification preferences.
 */
const updateNotificationPrefs = async (req, res) => {
  const userId = req.user.id;
  const { push, email, sms } = req.body;

  try {
    await db.query(
      "UPDATE users SET notification_prefs = $1 WHERE id = $2",
      [JSON.stringify({ push, email, sms }), userId]
    );
    res.status(200).json({ message: 'Preferences updated' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to update preferences' });
  }
};

/**
 * CRUD for Saved Recipients.
 */
const getRecipients = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query("SELECT * FROM saved_recipients WHERE user_id = $1 ORDER BY created_at DESC", [userId]);
    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch recipients' });
  }
};

const addRecipient = async (req, res) => {
  const userId = req.user.id;
  const { name, phone, label } = req.body;

  try {
    const { rows } = await db.query(
      "INSERT INTO saved_recipients (user_id, name, phone, label) VALUES ($1, $2, $3, $4) RETURNING *",
      [userId, name, phone, label]
    );
    res.status(201).json(rows[0]);
  } catch (error) {
    res.status(500).json({ error: 'Failed to save recipient' });
  }
};

const deleteRecipient = async (req, res) => {
    const { id } = req.params;
    const userId = req.user.id;
    try {
        await db.query("DELETE FROM saved_recipients WHERE id = $1 AND user_id = $2", [id, userId]);
        res.status(200).json({ message: 'Recipient deleted' });
    } catch (error) {
        res.status(500).json({ error: 'Failed to delete recipient' });
    }
};

/**
 * Manage Active Sessions.
 */
const getSessions = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
      "SELECT id, device_name, ip_address, last_active, created_at FROM user_sessions WHERE user_id = $1 ORDER BY last_active DESC",
      [userId]
    );
    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch sessions' });
  }
};

const revokeSession = async (req, res) => {
  const { id } = req.params;
  const userId = req.user.id;
  try {
    await db.query("DELETE FROM user_sessions WHERE id = $1 AND user_id = $2", [id, userId]);
    res.status(200).json({ message: 'Session revoked' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to revoke session' });
  }
};

/**
 * NDPA-compliant deletion request.
 */
const requestDeletion = async (req, res) => {
  const userId = req.user.id;
  try {
    await db.query("UPDATE users SET deletion_requested_at = CURRENT_TIMESTAMP WHERE id = $1", [userId]);
    res.status(200).json({ message: 'Account deletion request queued. Our team will process this within 30 days.' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to process request' });
  }
};

module.exports = {
  updateProfile,
  updateNotificationPrefs,
  getRecipients,
  addRecipient,
  deleteRecipient,
  getSessions,
  revokeSession,
  requestDeletion
};
