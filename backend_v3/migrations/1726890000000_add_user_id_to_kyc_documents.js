exports.up = (pgm) => {
  pgm.sql(`
    ALTER TABLE "kyc_documents" ALTER COLUMN "fulfiller_id" DROP NOT NULL;

    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'kyc_documents' AND column_name = 'user_id'
      ) THEN
        ALTER TABLE "kyc_documents" ADD COLUMN "user_id" integer REFERENCES "users" ON DELETE CASCADE;
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "kyc_documents" DROP COLUMN IF EXISTS "user_id";
  `);
};
