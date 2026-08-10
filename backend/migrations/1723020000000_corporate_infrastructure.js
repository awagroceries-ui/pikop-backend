exports.up = (pgm) => {
  // 1. Corporate Accounts Table
  pgm.createTable('corporate_accounts', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    company_name: { type: 'varchar(255)', notNull: true },
    billing_email: { type: 'varchar(255)', notNull: true },
    billing_type: { type: 'varchar(20)', notNull: true }, // 'direct_debit', 'prepaid_wallet'
    paystack_mandate_id: { type: 'varchar(255)', nullable: true },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' }, // 'pending', 'active', 'suspended'
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 2. Corporate Sub-Accounts Table
  pgm.createTable('corporate_sub_accounts', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    corporate_account_id: {
      type: 'uuid',
      notNull: true,
      references: '"corporate_accounts"',
      onDelete: 'cascade'
    },
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
      onDelete: 'cascade'
    },
    role: { type: 'varchar(20)', notNull: true, default: 'staff' }, // 'staff', 'billing_admin'
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  pgm.createIndex('corporate_sub_accounts', ['corporate_account_id', 'user_id'], { unique: true });

  // 3. Add Corporate columns to Orders and Wallets
  pgm.addColumns('orders', {
    corporate_account_id: { type: 'uuid', references: '"corporate_accounts"', onDelete: 'set null' }
  });

  pgm.addColumns('wallets', {
    corporate_account_id: { type: 'uuid', references: '"corporate_accounts"', onDelete: 'cascade' }
  });

  // Note: owner_type for corporate wallets will be 'CORPORATE'
};

exports.down = (pgm) => {
  pgm.removeColumns('wallets', ['corporate_account_id']);
  pgm.removeColumns('orders', ['corporate_account_id']);
  pgm.dropTable('corporate_sub_accounts');
  pgm.dropTable('corporate_accounts');
};
