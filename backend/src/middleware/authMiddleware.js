const jwt = require('jsonwebtoken');
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'pikop_secret';

/**
 * Authenticates the JWT token from the Authorization header.
 */
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) return res.status(401).json({ error: 'Access token required' });

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(401).json({ error: 'Invalid or expired token' });
    req.user = user;
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
