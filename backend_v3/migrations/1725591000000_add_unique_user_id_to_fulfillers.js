exports.up = (pgm) => {
  // Use raw SQL to check for constraint existence before adding it
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fulfillers_user_id_unique') THEN
        ALTER TABLE fulfillers ADD CONSTRAINT fulfillers_user_id_unique UNIQUE (user_id);
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.dropConstraint('fulfillers', 'fulfillers_user_id_unique');
};
