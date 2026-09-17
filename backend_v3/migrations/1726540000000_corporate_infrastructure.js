exports.up = (pgm) => {
  // 1. Extend Users Role Constraint
  pgm.dropConstraint('users', 'users_role_check');
  pgm.addConstraint('users', 'users_role_check', {
    check: "role IN ('CUSTOMER', 'FULFILLER', 'MERCHANT', 'FLEET_PARTNER', 'CORPORATE', 'ADMIN', 'SUPER_ADMIN')"
  });

  // 2. Corporate Accounts Table
  pgm.createTable('corporate_accounts', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    owner_user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    company_name: { type: 'varchar(255)', notNull: true },
    cac_number: { type: 'varchar(50)', unique: true },
    business_address: { type: 'text' },
    billing_email: { type: 'varchar(255)', notNull: true },
    billing_type: { type: 'varchar(20)', notNull: true, default: 'prepaid_wallet' }, // prepaid_wallet, post_paid
    status: { type: 'varchar(20)', notNull: true, default: 'PENDING_VERIFICATION' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });

  // 3. Corporate Sub-Accounts (Staff/Authorized Users)
  pgm.createTable('corporate_sub_accounts', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    corporate_account_id: { type: 'uuid', notNull: true, references: '"corporate_accounts"', onDelete: 'cascade' },
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    role: { type: 'varchar(20)', notNull: true, default: 'STAFF' }, // STAFF, ADMIN
    daily_spend_limit: { type: 'decimal(12,2)', default: 0.0 }, // 0 = unlimited for now
    monthly_spend_limit: { type: 'decimal(12,2)', default: 0.0 },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  // 4. Link Orders & Wallets to Corporate
  pgm.addColumns('orders', {
    corporate_account_id: { type: 'uuid', references: '"corporate_accounts"', onDelete: 'set null' }
  });

  pgm.addColumns('wallets', {
    corporate_account_id: { type: 'uuid', references: '"corporate_accounts"', onDelete: 'cascade' }
  });

  pgm.createIndex('corporate_accounts', 'owner_user_id');
  pgm.createIndex('corporate_sub_accounts', ['corporate_account_id', 'user_id'], { unique: true });
  pgm.createIndex('orders', 'corporate_account_id');
};

exports.down = (pgm) => {
  pgm.removeColumns('wallets', ['corporate_account_id']);
  pgm.removeColumns('orders', ['corporate_account_id']);
  pgm.dropTable('corporate_sub_accounts');
  pgm.dropTable('corporate_accounts');
};
