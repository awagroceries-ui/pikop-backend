const { Pool } = require('pg');
require('dotenv').config();

const connectionString = process.env.DATABASE_URL;

// 1. Pre-validation and Debugging (Doesn't log the password)
if (!connectionString) {
  console.error('❌ CRITICAL: DATABASE_URL is not defined in environment variables.');
} else {
    try {
        // Attempt to parse manually to detect issues that pg might trip over
        const url = new URL(connectionString);
        if (!url.password) {
            console.error('❌ DATABASE_URL Error: No password found in connection string. If your password has special characters like @ or #, you must URL-encode them.');
        }
    } catch (e) {
        console.error('❌ DATABASE_URL Error: Invalid URL format provided.');
    }
}

// 2. Optimized Pool Configuration
const pool = new Pool({
  connectionString: connectionString,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000, // Higher timeout for remote connections
  // Automated SSL handling for common production providers (AWS, Heroku, Supabase, Neon)
  ssl: (connectionString && (
      connectionString.includes('sslmode=require') ||
      connectionString.includes('supabase') ||
      connectionString.includes('neon.tech')
  )) ? { rejectUnauthorized: false } : false
});

// 3. Health Check
pool.on('connect', (client) => {
    // Handshake successful
});

pool.on('error', (err) => {
    console.error('[Database] Idle client error:', err.message);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
