exports.up = (pgm) => {
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vendors' AND column_name = 'approved_at'
      ) THEN
        ALTER TABLE "vendors" ADD COLUMN "approved_at" timestamp;
      END IF;

      IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'kitchens' AND column_name = 'approved_at'
      ) THEN
        ALTER TABLE "kitchens" ADD COLUMN "approved_at" timestamp;
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "vendors" DROP COLUMN IF EXISTS "approved_at";
    ALTER TABLE "kitchens" DROP COLUMN IF EXISTS "approved_at";
  `);
};
