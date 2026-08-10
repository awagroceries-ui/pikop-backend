const db = require('../config/db');

/**
 * Validates a promo code for the current user.
 */
const validateCode = async (req, res) => {
  const { code } = req.body;
  const userId = req.user.id;

  try {
    const { rows } = await db.query(`
      SELECT * FROM promo_codes
      WHERE code = $1
      AND valid_from <= CURRENT_TIMESTAMP
      AND valid_to >= CURRENT_TIMESTAMP
      AND used_count < max_uses`, [code.toUpperCase()]);

    if (rows.length === 0) {
      return res.status(404).json({ message: 'Promo code invalid or expired.' });
    }

    const promo = rows[0];

    // Check if user already used it
    const { rows: usedRes } = await db.query(
      "SELECT id FROM promo_code_redemptions WHERE promo_code_id = $1 AND user_id = $2",
      [promo.id, userId]
    );

    if (usedRes.length > 0) {
      return res.status(400).json({ message: 'You have already used this promo code.' });
    }

    res.status(200).json({
      promo_id: promo.id,
      discount_type: promo.discount_type,
      value: parseFloat(promo.value),
      message: 'Promo code applied!'
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to validate promo code' });
  }
};

module.exports = {
  validateCode
};
