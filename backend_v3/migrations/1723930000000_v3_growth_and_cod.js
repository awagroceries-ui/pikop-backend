exports.up = (pgm) => {
  // 1. Extend Orders with CoD columns
  pgm.addColumns('orders', {
    collect_on_delivery_amount: { type: 'decimal(15,2)' },
    collection_status: {
      type: 'varchar(20)',
      notNull: true,
      default: 'not_applicable',
      check: "collection_status IN ('not_applicable', 'pending', 'collected', 'failed')"
    },
    collection_payment_reference: { type: 'varchar(255)' },
    collection_method: { type: 'varchar(20)' }, // 'qr', 'transfer', 'card'
  });

  // 2. Coupons Table
  pgm.createTable('coupons', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    code: { type: 'varchar(50)', notNull: true, unique: true },
    discount_type: { type: 'varchar(20)', notNull: true, check: "discount_type IN ('FIXED', 'PERCENTAGE')" },
    discount_value: { type: 'decimal(12,2)', notNull: true },
    min_order_amount: { type: 'decimal(12,2)', notNull: true, default: 0 },
    max_discount_amount: { type: 'decimal(12,2)' },
    expiry_at: { type: 'timestamp' },
    usage_limit: { type: 'integer' },
    usage_count: { type: 'integer', default: 0 },
    is_active: { type: 'boolean', default: true },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 3. Referrals Table
  pgm.createTable('referrals', {
    id: 'id',
    referrer_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    referred_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    status: { type: 'varchar(20)', notNull: true, default: 'pending', check: "status IN ('pending', 'completed')" },
    rewarded_at: { type: 'timestamp' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 4. Loyalty Ledger
  pgm.createTable('loyalty_ledger', {
    id: 'id',
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    points: { type: 'integer', notNull: true },
    entry_type: { type: 'varchar(10)', notNull: true, check: "entry_type IN ('EARN', 'REDEEM')" },
    description: { type: 'text' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  pgm.createIndex('coupons', 'code');
  pgm.createIndex('referrals', 'referrer_id');
  pgm.createIndex('loyalty_ledger', 'user_id');
};

exports.down = (pgm) => {
  pgm.dropTable('loyalty_ledger');
  pgm.dropTable('referrals');
  pgm.dropTable('coupons');
  pgm.removeColumns('orders', ['collection_method', 'collection_payment_reference', 'collection_status', 'collect_on_delivery_amount']);
};
