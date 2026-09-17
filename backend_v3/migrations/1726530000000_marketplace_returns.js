exports.up = (pgm) => {
  // 1. Extend Merchant Profiles with Return Policy
  pgm.addColumns('vendors', {
    allows_returns: { type: 'boolean', notNull: true, default: false },
    return_window_days: { type: 'integer', notNull: true, default: 7 },
    return_policy_text: { type: 'text' }
  });

  pgm.addColumns('kitchens', {
    allows_returns: { type: 'boolean', notNull: true, default: false },
    return_window_days: { type: 'integer', notNull: true, default: 7 },
    return_policy_text: { type: 'text' }
  });

  // 2. Returns Table
  pgm.createTable('returns', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    order_id: { type: 'integer', notNull: true, references: '"orders"', onDelete: 'cascade' },
    status: {
      type: 'varchar(30)',
      notNull: true,
      default: 'PENDING',
      check: "status IN ('PENDING', 'APPROVED', 'DECLINED', 'IN_TRANSIT', 'RECEIVED', 'COMPLETED', 'CANCELLED')"
    },
    reason: { type: 'text', notNull: true },
    evidence_urls: { type: 'text[]' },
    merchant_notes: { type: 'text' },
    return_delivery_order_id: { type: 'integer', references: '"orders"', onDelete: 'set null' },
    delivery_fee_payer: {
      type: 'varchar(20)',
      notNull: true,
      default: 'CUSTOMER',
      check: "delivery_fee_payer IN ('CUSTOMER', 'MERCHANT')"
    },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  pgm.createIndex('returns', 'order_id');
  pgm.createIndex('returns', 'status');
};

exports.down = (pgm) => {
  pgm.dropTable('returns');
  pgm.dropColumns('kitchens', ['allows_returns', 'return_window_days', 'return_policy_text']);
  pgm.dropColumns('vendors', ['allows_returns', 'return_window_days', 'return_policy_text']);
};
