const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'pikop_secret_v3_core';
const REFRESH_TOKEN_SECRET = process.env.REFRESH_TOKEN_SECRET || 'pikop_refresh_v3_core';

/**
 * Hashes a plain text password.
 */
const hashPassword = async (password) => {
  const salt = await bcrypt.genSalt(10);
  return bcrypt.hash(password, salt);
};

/**
 * Compares a plain text password with a hash.
 */
const comparePassword = async (password, hash) => {
  return bcrypt.compare(password, hash);
};

/**
 * Generates Access and Refresh tokens for a user.
 */
const generateTokens = (user) => {
  const payload = {
    id: user.id,
    email: user.email,
    role: user.role
  };

  const accessToken = jwt.sign(payload, JWT_SECRET, { expiresIn: '1d' });
  const refreshToken = jwt.sign({ id: user.id }, REFRESH_TOKEN_SECRET, { expiresIn: '30d' });

  return { accessToken, refreshToken };
};

module.exports = {
  hashPassword,
  comparePassword,
  generateTokens,
  JWT_SECRET,
  REFRESH_TOKEN_SECRET
};
