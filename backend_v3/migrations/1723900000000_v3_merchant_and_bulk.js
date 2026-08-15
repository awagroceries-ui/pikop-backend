exports.up = (pgm) => {
  // 1. Merchant Accounts Table
  pgm.createTable('merchant_accounts', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    business_name: { type: 'varchar(255)', notNull: true },
    contact_email: { type: 'varchar(255)', notNull: true, unique: true },
    api_key_hash: { type: 'text', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'active' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 2. Merchant Sub-Accounts
  pgm.createTable('merchant_sub_accounts', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    merchant_account_id: { type: 'uuid', references: '"merchant_accounts"', onDelete: 'cascade' },
    user_id: { type: 'integer', references: '"users"', onDelete: 'cascade' },
    role: { type: 'varchar(20)', notNull: true, default: 'staff' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 3. Order Batches (For Bulk Ordering)
  pgm.createTable('order_batches', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    merchant_account_id: { type: 'uuid', references: '"merchant_accounts"', onDelete: 'cascade' },
    name: { type: 'varchar(255)' },
    status: { type: 'varchar(20)', notNull: true, default: 'processing' },
    total_orders: { type: 'integer', default: 0 },
    processed_orders: { type: 'integer', default: 0 },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 4. Update Orders Table with Merchant and Batch support
  pgm.addColumns('orders', {
    merchant_account_id: { type: 'uuid', references: '"merchant_accounts"', onDelete: 'set null' },
    batch_id: { type: 'uuid', references: '"order_batches"', onDelete: 'set null' },
    scheduled_at: { type: 'timestamp' },
    declared_value: { type: 'decimal(12,2)' },
    waybill_url: { type: 'text' },
  });

  pgm.createIndex('merchant_sub_accounts', ['merchant_account_id', 'user_id'], { unique: true });
  pgm.createIndex('orders', 'batch_id');
  pgm.createIndex('orders', 'merchant_account_id');
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['waybill_url', 'declared_value', 'scheduled_at', 'batch_id', 'merchant_account_id']);
  pgm.dropTable('order_batches');
  pgm.dropTable('merchant_sub_accounts');
  pgm.dropTable('merchant_accounts');
};
