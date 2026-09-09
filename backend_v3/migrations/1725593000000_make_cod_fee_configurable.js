exports.up = (pgm) => {
  // Initialize COD Platform Fee setting if not exists
  pgm.sql(`
    INSERT INTO settings (key, value)
    VALUES ('cod_fee_rate', '0.10')
    ON CONFLICT (key) DO NOTHING;
  `);
};

exports.down = (pgm) => {
  // We usually don't delete settings on down to avoid breaking app,
  // but if we must:
  // pgm.sql("DELETE FROM settings WHERE key = 'cod_fee_rate'");
};
