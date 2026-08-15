exports.up = (pgm) => {
  // 1. Wallets Table
  pgm.createTable('wallets', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    owner_type: {
      type: 'varchar(20)',
      notNull: true,
      check: "owner_type IN ('USER', 'FULFILLER', 'PLATFORM', 'CORPORATE', 'MERCHANT', 'VENDOR', 'KITCHEN')"
    },
    owner_id: { type: 'varchar(255)', notNull: true }, // ID from respective tables
    balance: { type: 'decimal(15,2)', notNull: true, default: 0 },
    currency: { type: 'varchar(10)', notNull: true, default: 'NGN' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
    updated_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 2. Ledger Entries
  pgm.createTable('wallet_ledger_entries', {
    id: 'id',
    wallet_id: { type: 'uuid', notNull: true, references: '"wallets"', onDelete: 'cascade' },
    order_id: { type: 'integer', references: '"orders"', onDelete: 'set null' },
    entry_type: { type: 'varchar(10)', notNull: true, check: "entry_type IN ('CREDIT', 'DEBIT')" },
    amount: { type: 'decimal(15,2)', notNull: true },
    balance_after: { type: 'decimal(15,2)', notNull: true },
    purpose: { type: 'varchar(50)', notNull: true }, // MISSION_PAYMENT, SETTLEMENT, WITHDRAWAL, etc.
    description: { type: 'text' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 3. Withdrawals Table
  pgm.createTable('withdrawals', {
    id: 'id',
    fulfiller_id: { type: 'integer', references: '"fulfillers"', onDelete: 'cascade' },
    wallet_id: { type: 'uuid', references: '"wallets"' },
    amount: { type: 'decimal(12,2)', notNull: true },
    fee: { type: 'decimal(12,2)', notNull: true, default: 0 },
    status: { type: 'varchar(20)', notNull: true, default: 'PENDING' },
    paystack_transfer_code: { type: 'varchar(255)' },
    bank_account_id: { type: 'uuid' }, // Future: point to bank_accounts table
    requested_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
    processed_at: { type: 'timestamp' },
  });

  pgm.createIndex('wallets', ['owner_type', 'owner_id'], { unique: true });
  pgm.createIndex('wallet_ledger_entries', 'wallet_id');
  pgm.createIndex('withdrawals', 'fulfiller_id');
};

exports.down = (pgm) => {
  pgm.dropTable('withdrawals');
  pgm.dropTable('wallet_ledger_entries');
  pgm.dropTable('wallets');
};
