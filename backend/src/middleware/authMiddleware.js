const jwt = require('jsonwebtoken');
const db = require('../config/db');
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'pikop_secret';

/**
 * Authenticates the JWT token from the Authorization header.
 */
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) return res.status(401).json({ error: 'Access token required' });

  jwt.verify(token, JWT_SECRET, async (err, user) => {
    if (err) return res.status(401).json({ error: 'Invalid or expired token' });
    req.user = user;

    // Track Activity (Prompt 4)
    // Throttled to once every 5 minutes
    try {
        const fiveMinsAgo = new Date(Date.now() - 5 * 60000);
        await db.query(
            "UPDATE users SET last_active_at = CURRENT_TIMESTAMP WHERE id = $1 AND (last_active_at IS NULL OR last_active_at < $2)",
            [user.id, fiveMinsAgo]
        );
        if (user.role === 'FULFILLER') {
            await db.query(
                "UPDATE fulfillers SET last_active_at = CURRENT_TIMESTAMP WHERE user_id = $1 AND (last_active_at IS NULL OR last_active_at < $2)",
                [user.id, fiveMinsAgo]
            );
        }
    } catch (e) {
        console.error("Activity tracking failed:", e.message);
    }

    next();
  });
};

/**
 * Ensures the user has a verified email.
 */
const requireEmailVerified = (req, res, next) => {
  if (!req.user) return res.status(401).json({ error: 'Authentication required' });

  // Note: We should ideally fetch the latest verified status from DB or include it in JWT
  if (!req.user.isVerified) {
    return res.status(403).json({ error: 'Email verification required' });
  }
  next();
};

module.exports = {
  authenticateToken,
  requireEmailVerified
};
