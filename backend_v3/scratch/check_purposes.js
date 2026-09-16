const db = require('../src/config/db');

async function check() {
  try {
    const { rows } = await db.query("SELECT DISTINCT purpose FROM wallet_ledger_entries");
    console.log('Unique Purposes in DB:');
    rows.forEach(r => console.log(`'${r.purpose}'`));
    process.exit(0);
  } catch (e) {
    console.error(e);
    process.exit(1);
  }
}

check();
