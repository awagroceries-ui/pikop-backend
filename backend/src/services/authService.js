const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'pikop_secret';
const REFRESH_TOKEN_SECRET = process.env.REFRESH_TOKEN_SECRET || 'pikop_refresh_secret';

/**
 * Generates Access and Refresh tokens for a user.
 */
const generateTokens = (user) => {
  const payload = {
    id: user.id,
    email: user.email,
    role: user.role,
    isVerified: !!user.email_verified_at
  };

  const accessToken = jwt.sign(payload, JWT_SECRET, { expiresIn: '15m' });
  const refreshToken = jwt.sign(payload, REFRESH_TOKEN_SECRET, { expiresIn: '7d' });

  return { accessToken, refreshToken };
};

/**
 * Hashes a plain text password.
 */
const hashPassword = async (password) => {
  const saltRounds = 10;
  return await bcrypt.hash(password, saltRounds);
};

/**
 * Compares a plain text password with a hash.
 */
const comparePassword = async (password, hash) => {
  return await bcrypt.compare(password, hash);
};

module.exports = {
  generateTokens,
  hashPassword,
  comparePassword,
  JWT_SECRET,
  REFRESH_TOKEN_SECRET
};
