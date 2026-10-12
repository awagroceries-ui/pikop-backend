exports.up = (pgm) => {
  // v4.5 fix: merchants (vendors/kitchens) had no columns to store payout bank
  // details at all, so withdrawals silently failed for every merchant.
  // Mirrors the same columns already present on the fulfillers table.
  pgm.sql(`
    ALTER TABLE "vendors"
      ADD COLUMN IF NOT EXISTS "bank_name" text,
      ADD COLUMN IF NOT EXISTS "account_number" text,
      ADD COLUMN IF NOT EXISTS "bank_code" text,
      ADD COLUMN IF NOT EXISTS "account_name" text;

    ALTER TABLE "kitchens"
      ADD COLUMN IF NOT EXISTS "bank_name" text,
      ADD COLUMN IF NOT EXISTS "account_number" text,
      ADD COLUMN IF NOT EXISTS "bank_code" text,
      ADD COLUMN IF NOT EXISTS "account_name" text;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "vendors"
      DROP COLUMN IF EXISTS "bank_name",
      DROP COLUMN IF EXISTS "account_number",
      DROP COLUMN IF EXISTS "bank_code",
      DROP COLUMN IF EXISTS "account_name";

    ALTER TABLE "kitchens"
      DROP COLUMN IF EXISTS "bank_name",
      DROP COLUMN IF EXISTS "account_number",
      DROP COLUMN IF EXISTS "bank_code",
      DROP COLUMN IF EXISTS "account_name";
  `);
};
