exports.up = (pgm) => {
  // 1. User phone verification tracking
  pgm.addColumns('users', {
    phone_verified_at: { type: 'timestamp' }
  });

  // 2. SMS charging infrastructure
  pgm.addColumns('quotes', {
    sms_charge_amount: { type: 'decimal(10,2)', notNull: true, default: 0 }
  });
  pgm.addColumns('orders', {
    sms_charge_amount: { type: 'decimal(10,2)', notNull: true, default: 0 }
  });

  // 3. SMS Audit Ledger
  pgm.createTable('sms_logs', {
    id: 'id',
    recipient: { type: 'varchar(20)', notNull: true },
    content: { type: 'text', notNull: true },
    purpose: { type: 'varchar(50)', notNull: true }, // signup_otp, payment_link, tracking_link
    order_id: { type: 'integer', references: '"orders"', onDelete: 'set null' },
    status: { type: 'varchar(20)', notNull: true, default: 'sent' }, // sent, delivered, failed
    provider_ref: { type: 'varchar(255)' },
    cost_naira: { type: 'decimal(10,2)', notNull: true, default: 0 },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  pgm.createIndex('sms_logs', 'recipient');
  pgm.createIndex('sms_logs', 'order_id');
};

exports.down = (pgm) => {
  pgm.dropTable('sms_logs');
  pgm.dropColumns('orders', ['sms_charge_amount']);
  pgm.dropColumns('quotes', ['sms_charge_amount']);
  pgm.dropColumns('users', ['phone_verified_at']);
};
