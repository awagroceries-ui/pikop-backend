const { Pool } = require('pg');
const path = require('path');
const url = require('url');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

let connString = process.env.DATABASE_URL;

// Ensure IPv4 for local connections
if (connString && (connString.includes('localhost') || connString.includes('127.0.0.1'))) {
  try {
    const parsedUrl = new url.URL(connString);
    parsedUrl.hostname = '127.0.0.1';
    connString = parsedUrl.toString();
  } catch (e) {
    console.error('Database URL Parsing Error:', e.message);
  }
}

const pool = new Pool({
  connectionString: connString,
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
