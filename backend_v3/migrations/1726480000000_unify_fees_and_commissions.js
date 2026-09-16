exports.up = (pgm) => {
  // 1. Add dispatch_commission_amount to orders to freeze the Pikop share at creation
  pgm.addColumns('orders', {
    dispatch_commission_amount: { type: 'decimal(12,2)', default: 0.0 }
  });

  // 2. Seed Guest SMS Charge and ensure platform_commission is initialized
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
    ('guest_sms_charge', '50'),
    ('platform_commission', '0.25')
    ON CONFLICT (key) DO NOTHING;
  `);
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['dispatch_commission_amount']);
  pgm.sql(`DELETE FROM settings WHERE key = 'guest_sms_charge';`);
};
