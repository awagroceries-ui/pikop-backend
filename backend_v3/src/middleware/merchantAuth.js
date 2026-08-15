const db = require('../config/db');
const crypto = require('crypto');

/**
 * Middleware to authenticate requests via Merchant API Key.
 */
const authenticateMerchantKey = async (req, res, next) => {
  const apiKey = req.headers['x-pikop-api-key'];
  if (!apiKey) return res.status(401).json({ success: false, message: 'Merchant API key required' });

  try {
    // Hash the incoming key to compare with stored hash
    const hash = crypto.createHash('sha256').update(apiKey).digest('hex');

    const { rows } = await db.query(
      "SELECT id, business_name FROM merchant_accounts WHERE api_key_hash = $1 AND status = 'active'",
      [hash]
    );

    if (rows.length === 0) {
      return res.status(403).json({ success: false, message: 'Invalid or inactive Merchant API key' });
    }

    req.merchant = rows[0];
    next();
  } catch (error) {
    console.error('[MerchantAuth] Error:', error.message);
    res.status(500).json({ success: false, message: 'Authentication error' });
  }
};

module.exports = { authenticateMerchantKey };
