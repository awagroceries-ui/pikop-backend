exports.up = (pgm) => {
  // 1. Extend Wallets with pending_balance
  pgm.addColumns('wallets', {
    pending_balance: { type: 'decimal(15,2)', notNull: true, default: 0 }
  });

  // 2. Extend Orders with Escrow and Fee details
  pgm.addColumns('orders', {
    item_price: { type: 'decimal(12,2)', notNull: true, default: 0 },
    delivery_fee: { type: 'decimal(12,2)', notNull: true, default: 0 },
    platform_fee_amount: { type: 'decimal(12,2)', notNull: true, default: 0 },
    initiator_role: {
      type: 'varchar(20)',
      check: "initiator_role IN ('PAYER', 'SELLER')"
    },
    fee_payer: {
      type: 'varchar(20)',
      check: "fee_payer IN ('PAYER', 'SELLER')"
    },
    seller_id: { type: 'integer', references: '"users"', onDelete: 'set null' },
    seller_phone: { type: 'varchar(20)' },
    escrow_status: {
      type: 'varchar(30)',
      notNull: true,
      default: 'not_applicable',
      check: "escrow_status IN ('not_applicable', 'held', 'released', 'disputed', 'refunded')"
    },
    grace_period_expires_at: { type: 'timestamp' }
  });

  // 3. Add metadata to Ledger for itemized breakdown
  pgm.addColumns('wallet_ledger_entries', {
    metadata: { type: 'jsonb' }
  });

  // 4. Update order status check constraint to include new states
  // Note: pg-migrate doesn't have a direct 'alterCheck' helper for all DBs,
  // so we'll drop and recreate if necessary, or just use raw SQL for reliability.
  pgm.sql(`
    ALTER TABLE "orders" DROP CONSTRAINT IF EXISTS "orders_status_check";
    ALTER TABLE "orders" ADD CONSTRAINT "orders_status_check"
    CHECK (status IN (
      'SEARCHING', 'MATCHED', 'QUEUED', 'PICKED_UP', 'IN_TRANSIT',
      'ARRIVED_AT_DELIVERY', 'DELIVERED', 'CANCELLED', 'RECIPIENT_ABSENT',
      'PAYMENT_PENDING', 'PAYMENT_CAPTURED', 'DELIVERED_PENDING_CONFIRMATION',
      'CONFIRMED', 'DISPUTED', 'REFUNDED', 'RELEASED'
    ));
  `);
};

exports.down = (pgm) => {
  pgm.removeColumns('wallet_ledger_entries', ['metadata']);
  pgm.removeColumns('orders', [
    'item_price', 'delivery_fee', 'platform_fee_amount',
    'initiator_role', 'fee_payer', 'escrow_status', 'grace_period_expires_at'
  ]);
  pgm.removeColumns('wallets', ['pending_balance']);
};
