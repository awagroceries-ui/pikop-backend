const { Pool } = require('pg');
const path = require('path');
// Explicitly load .env from project root
require('dotenv').config({ path: path.join(process.cwd(), '.env') });

const connectionString = process.env.DATABASE_URL;

// 1. Pre-validation and Environmental Audit
console.log('[Database] Initializing connection pool...');
console.log('[Database] Environment check:', {
    has_db_url: !!connectionString,
    node_env: process.env.NODE_ENV,
    cwd: process.cwd()
});

if (!connectionString) {
  console.error('❌ CRITICAL: DATABASE_URL is not defined. Check your .env file.');
}

const pool = new Pool({
  connectionString: connectionString,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
  // Handle SCRAM authentication by ensuring password is a string if connectionString exists
  password: connectionString ? (new URL(connectionString).password || '') : undefined,
  ssl: (connectionString && (
      connectionString.includes('sslmode=require') ||
      connectionString.includes('supabase') ||
      connectionString.includes('neon.tech')
  )) ? { rejectUnauthorized: false } : false
});

// 3. Handshake logic
pool.on('connect', () => {
    // Client connected
});

pool.on('error', (err) => {
    console.error('[Database] Idle client error:', err.message);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
