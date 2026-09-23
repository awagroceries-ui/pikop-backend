exports.up = (pgm) => {
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'corporate_accounts_owner_user_id_key'
      ) THEN
        ALTER TABLE "corporate_accounts" ADD CONSTRAINT "corporate_accounts_owner_user_id_key" UNIQUE ("owner_user_id");
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "corporate_accounts" DROP CONSTRAINT IF EXISTS "corporate_accounts_owner_user_id_key";
  `);
};
