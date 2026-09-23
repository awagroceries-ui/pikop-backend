exports.up = (pgm) => {
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'corporate_accounts' AND column_name = 'cac_document_url'
      ) THEN
        ALTER TABLE "corporate_accounts" ADD COLUMN "cac_document_url" text;
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "corporate_accounts" DROP COLUMN IF EXISTS "cac_document_url";
  `);
};
