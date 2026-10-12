exports.up = (pgm) => {
  // v4.5 fix: an earlier migration (1725577103000_escrow_wallet_system)
  // already adds this same column, so on a fresh database this migration
  // was failing with "column payer_id already exists" and aborting the
  // whole migration run (discovered while reproducing issues #10/#11 on a
  // clean test database). Guard with IF NOT EXISTS so `node-pg-migrate up`
  // is safe to run from scratch.
  pgm.sql(`ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS "payer_id" integer REFERENCES "users" ON DELETE SET NULL;`);
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['payer_id']);
};
