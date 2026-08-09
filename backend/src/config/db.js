const { Pool } = require('pg');
const path = require('path');
const url = require('url');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

let connString = process.env.DATABASE_URL;

// Force IPv4 for local connections (bypasses IPv6 ::1 issues on VPS)
if (connString) {
  connString = connString.replace('localhost', '127.0.0.1').replace('[::1]', '127.0.0.1');
}

const pool = new Pool({
  connectionString: connString,
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
