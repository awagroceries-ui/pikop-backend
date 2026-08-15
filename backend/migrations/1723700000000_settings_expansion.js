exports.up = (pgm) => {
  // 1. Add base fare rates to settings
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
    ('base_fare_small', '500'),
    ('base_fare_medium', '1000'),
    ('base_fare_large', '1500'),
    ('per_km_rate', '150')
    ON CONFLICT (key) DO NOTHING;
  `);

  // 2. Fix messages table if 'body' was intended or just keep using 'content'
  // I will stick to 'content' as it is already in the main migration
};

exports.down = (pgm) => {
  pgm.sql("DELETE FROM settings WHERE key IN ('base_fare_small', 'base_fare_medium', 'base_fare_large', 'per_km_rate')");
};
