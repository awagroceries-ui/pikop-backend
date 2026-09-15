exports.up = (pgm) => {
  // 1. Merchant Operating Hours
  pgm.addColumns('vendors', {
    operating_hours: { type: 'jsonb', default: '{"all": {"open": "08:00", "close": "20:00"}}' }
  });
  pgm.addColumns('kitchens', {
    operating_hours: { type: 'jsonb', default: '{"all": {"open": "08:00", "close": "20:00"}}' }
  });

  // 2. Order Scheduling
  pgm.addColumns('orders', {
    scheduled_at: { type: 'timestamp' }
  });
  pgm.createIndex('orders', 'scheduled_at');

  // 3. Daylight Dispatch Settings
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
    ('daylight_dispatch_start', '06:00'),
    ('daylight_dispatch_end', '18:00')
    ON CONFLICT (key) DO NOTHING;
  `);
};

exports.down = (pgm) => {
  pgm.dropColumns('vendors', ['operating_hours']);
  pgm.dropColumns('kitchens', ['operating_hours']);
  pgm.dropColumns('orders', ['scheduled_at']);
  pgm.sql(`DELETE FROM settings WHERE key IN ('daylight_dispatch_start', 'daylight_dispatch_end');`);
};