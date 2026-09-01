exports.up = (pgm) => {
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fulfillers' AND column_name='last_ping_at') THEN
        ALTER TABLE "fulfillers" ADD COLUMN "last_ping_at" timestamp;
      END IF;

      IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fulfillers' AND column_name='last_active_at') THEN
        ALTER TABLE "fulfillers" ADD COLUMN "last_active_at" timestamp;
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['last_ping_at', 'last_active_at']);
};
