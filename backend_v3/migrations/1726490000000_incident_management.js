exports.up = (pgm) => {
  // 1. Extend Disputes with structured fields
  pgm.addColumns('disputes', {
    severity: {
      type: 'varchar(20)',
      notNull: true,
      default: 'MEDIUM',
      check: "severity IN ('LOW', 'MEDIUM', 'HIGH')"
    },
    incident_category: { type: 'varchar(50)' }, // e.g., VEHICLE_BREAKDOWN, INCORRECT_ITEM
    is_3way_bridged: { type: 'boolean', notNull: true, default: false },
    assigned_admin_id: { type: 'integer', references: '"admin_users"', onDelete: 'set null' }
  });

  // 2. Create Conversation Participants Table (supports multi-party chat)
  pgm.createTable('conversation_participants', {
    id: 'id',
    conversation_id: { type: 'uuid', notNull: true, references: '"conversations"', onDelete: 'cascade' },
    user_id: { type: 'integer' }, // Null if participant is an Admin
    admin_id: { type: 'integer', references: '"admin_users"', onDelete: 'set null' },
    role: {
      type: 'varchar(20)',
      notNull: true,
      check: "role IN ('CUSTOMER', 'FULFILLER', 'ADMIN')"
    },
    joined_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  pgm.createIndex('conversation_participants', 'conversation_id');
  pgm.createIndex('conversation_participants', 'user_id');
  pgm.createIndex('conversation_participants', 'admin_id');

  // 3. Add Waiver logic support to Ledger
  pgm.sql(`
    ALTER TABLE "wallet_ledger_entries" DROP CONSTRAINT IF EXISTS "wallet_ledger_entries_purpose_check";
    ALTER TABLE "wallet_ledger_entries" ADD CONSTRAINT "wallet_ledger_entries_purpose_check"
    CHECK (purpose IN (
      'SETTLEMENT', 'COMMISSION', 'SECURE_PAY_FEE', 'SMS_CHARGE',
      'ESCROW_HOLD', 'ESCROW_RELEASE', 'ESCROW_REFUND',
      'TOPUP', 'WITHDRAWAL', 'REFERRAL_BONUS', 'REFERRAL_WELCOME',
      'CANCELLATION_PENALTY', 'PENALTY_WAIVER', 'RETURN_FEE', 'RETURN_WAIVER',
      'BULK_DISPATCH', 'COD_COLLECTION',
      'DELIVERY_PAYMENT', 'CANCELLATION_FEE', 'CORPORATE_ORDER',
      'DIRECT_DEBIT_ORDER', 'REFERRAL_REWARD', 'REFEREE_WELCOME'
    ));
  `);
};

exports.down = (pgm) => {
  pgm.dropTable('conversation_participants');
  pgm.dropColumns('disputes', ['severity', 'incident_category', 'is_3way_bridged', 'assigned_admin_id']);
};
