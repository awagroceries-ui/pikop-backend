exports.up = (pgm) => {
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
    ('food_commission', '0.10'),
    ('groceries_commission', '0.05'),
    ('shop_commission', '0.10')
    ON CONFLICT (key) DO NOTHING;
  `);
};

exports.down = (pgm) => {
  pgm.sql(`DELETE FROM settings WHERE key IN ('food_commission', 'groceries_commission', 'shop_commission');`);
};