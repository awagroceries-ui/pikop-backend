exports.up = (pgm) => {
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
      ('cod_fee_rate', '0.05'),
      ('platform_commission', '0.20'),
      ('food_commission', '0.05'),
      ('groceries_commission', '0.05'),
      ('shop_commission', '0.05')
    ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    UPDATE settings SET value = '0.10' WHERE key IN ('cod_fee_rate', 'food_commission', 'shop_commission');
    UPDATE settings SET value = '0.25' WHERE key = 'platform_commission';
    UPDATE settings SET value = '0.05' WHERE key = 'groceries_commission';
  `);
};
