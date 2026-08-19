const { Pool } = require('pg');
require('dotenv').config();

const connectionString = process.env.DATABASE_URL;

if (!connectionString) {
  console.error('❌ CRITICAL: DATABASE_URL is not defined in environment variables.');
}

/**
 * Simplified Connection Pool
 * We let the 'pg' library parse the connectionString directly.
 * This is the most reliable way to handle passwords with special characters like $.
 */
const pool = new Pool({
  connectionString: connectionString,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
  // Automatically enable SSL for production environments if specified in URL or env
  ssl: (connectionString && connectionString.includes('sslmode=require'))
       ? { rejectUnauthorized: false } : false
});

// Post-connection validation
pool.on('connect', () => {
    // Client connected successfully
});

pool.on('error', (err) => {
    console.error('[Database] Idle client error:', err.message);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
