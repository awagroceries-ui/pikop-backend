exports.up = (pgm) => {
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'kyc_provider_ref'
      ) THEN
        ALTER TABLE "users" ADD COLUMN "kyc_provider_ref" varchar(255);
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "users" DROP COLUMN IF EXISTS "kyc_provider_ref";
  `);
};
