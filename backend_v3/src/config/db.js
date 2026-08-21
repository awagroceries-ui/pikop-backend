const { Pool } = require('pg');
const parse = require('pg-connection-string').parse;
require('dotenv').config();

let connectionString = process.env.DATABASE_URL;

if (!connectionString) {
  console.error('❌ CRITICAL: DATABASE_URL is missing.');
}

/**
 * CLEANUP & DE-MANGLING
 * 1. Remove surrounding single/double quotes if they leaked into the variable
 * 2. Parse the URL into components
 */
if (connectionString) {
    connectionString = connectionString.trim().replace(/^['"]|['"]$/g, '');
}

const config = parse(connectionString || '');

// Host Sanitization: Prevent "base" ENOTFOUND error
if (config.host === 'base' || !config.host) {
    config.host = 'localhost';
}

// Ensure password is treated as a literal string to fix "SASL: password must be a string"
// Decodes URL-encoded characters (like %40 to @)
if (config.password) {
    config.password = decodeURIComponent(config.password);
}

const pool = new Pool({
  host: config.host || 'localhost',
  user: config.user || '',
  password: config.password || '',
  database: config.database || 'pikop',
  port: config.port || 5432,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
  ssl: (connectionString && (connectionString.includes('sslmode=require') || connectionString.includes('render.com')))
       ? { rejectUnauthorized: false } : false
});

// Host Sanitization: Prevent "base" ENOTFOUND error
if (pool.options.host === 'base') {
    pool.options.host = 'localhost';
}

console.log(`[Database] Target: ${pool.options.host}:${pool.options.port} | DB: ${pool.options.database}`);

pool.on('connect', () => {
    // Authenticated successfully
});

pool.on('error', (err) => {
    console.error('[Database] Unexpected error:', err.message);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
