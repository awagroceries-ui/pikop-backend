exports.shorthands = undefined;

exports.up = (pgm) => {
  // Wallets Table
  pgm.createTable('wallets', {
    id: 'id',
    owner_id: { type: 'integer' }, // Can be user_id, fulfiller_id, or null for platform
    owner_type: { type: 'varchar(20)', notNull: true }, // 'USER', 'FULFILLER', 'PLATFORM'
    balance: { type: 'numeric(12, 2)', notNull: true, default: 0 },
    currency: { type: 'varchar(3)', notNull: true, default: 'NGN' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
    updated_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Wallet Ledger Entries (Audit Trail)
  pgm.createTable('wallet_ledger_entries', {
    id: 'id',
    wallet_id: {
      type: 'integer',
      notNull: true,
      references: '"wallets"',
      onDelete: 'cascade',
    },
    amount: { type: 'numeric(12, 2)', notNull: true },
    entry_type: { type: 'varchar(10)', notNull: true }, // 'CREDIT', 'DEBIT'
    purpose: { type: 'varchar(50)', notNull: true }, // 'DELIVERY_PAYMENT', 'COMMISSION', 'WITHDRAWAL'
    reference_id: { type: 'varchar(255)' }, // order_id or withdrawal_id
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Withdrawals Table
  pgm.createTable('withdrawals', {
    id: 'id',
    fulfiller_id: {
      type: 'integer',
      notNull: true,
      references: '"fulfillers"',
    },
    wallet_id: {
      type: 'integer',
      notNull: true,
      references: '"wallets"',
    },
    amount: { type: 'numeric(12, 2)', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'PENDING' }, // 'PENDING', 'SUCCESSFUL', 'FAILED'
    paystack_transfer_code: { type: 'varchar(255)' },
    paystack_reference: { type: 'varchar(255)' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Create unique index for platform wallet
  pgm.createIndex('wallets', ['owner_type'], {
    where: "owner_type = 'PLATFORM'",
    unique: true,
    name: 'unique_platform_wallet'
  });

  // Insert Platform Wallet initially
  pgm.sql("INSERT INTO wallets (owner_type, balance) VALUES ('PLATFORM', 0)");
};

exports.down = (pgm) => {
  pgm.dropTable('withdrawals');
  pgm.dropTable('wallet_ledger_entries');
  pgm.dropTable('wallets');
};
