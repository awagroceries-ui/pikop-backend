const db = require('../src/config/db');

/**
 * Clears all tables to ensure test isolation.
 */
const clearDatabase = async () => {
  const tables = [
    'otp_verifications',
    'wallet_ledger_entries',
    'withdrawals',
    'quotes',
    'disputes',
    'orders',
    'fulfillers',
    'wallets',
    'users',
    'admin_users',
    'kyc_documents'
  ];

  for (const table of tables) {
    await db.query(`TRUNCATE TABLE "${table}" RESTART IDENTITY CASCADE`);
  }

  // Re-insert platform wallet
  await db.query("INSERT INTO wallets (owner_type, balance) VALUES ('PLATFORM', 0)");
};

module.exports = {
  clearDatabase
};
