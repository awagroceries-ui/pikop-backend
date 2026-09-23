exports.up = (pgm) => {
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'fulfillers' AND column_name = 'approved_at'
      ) THEN
        ALTER TABLE "fulfillers" ADD COLUMN "approved_at" timestamp;
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "fulfillers" DROP COLUMN IF EXISTS "approved_at";
  `);
};
