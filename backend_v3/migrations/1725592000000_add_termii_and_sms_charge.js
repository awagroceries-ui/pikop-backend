exports.up = (pgm) => {
  // Use raw SQL to add columns with existence checks for VPS reliability
  pgm.sql(`
    ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "phone_verified_at" TIMESTAMP;
    ALTER TABLE "quotes" ADD COLUMN IF NOT EXISTS "sms_charge_amount" DECIMAL(10,2) NOT NULL DEFAULT 0;
    ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS "sms_charge_amount" DECIMAL(10,2) NOT NULL DEFAULT 0;
  `);

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
  }, { ifNotExists: true });

  pgm.createIndex('sms_logs', 'recipient', { ifNotExists: true });
  pgm.createIndex('sms_logs', 'order_id', { ifNotExists: true });
};

exports.down = (pgm) => {
  pgm.dropTable('sms_logs');
  pgm.dropColumns('orders', ['sms_charge_amount']);
  pgm.dropColumns('quotes', ['sms_charge_amount']);
  pgm.dropColumns('users', ['phone_verified_at']);
};
