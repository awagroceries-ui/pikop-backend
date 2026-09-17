exports.up = (pgm) => {
  // 1. Extend Quotes
  pgm.addColumns('quotes', {
    surge_multiplier: { type: 'decimal(5,2)', notNull: true, default: 1.00 },
    insurance_fee: { type: 'decimal(12,2)', notNull: true, default: 0.00 }
  });

  // 2. Extend Orders
  pgm.addColumns('orders', {
    surge_multiplier: { type: 'decimal(5,2)', notNull: true, default: 1.00 },
    is_insured: { type: 'boolean', notNull: true, default: false },
    insurance_fee: { type: 'decimal(12,2)', notNull: true, default: 0.00 }
  });

  // 3. Extend Wallet Ledger Purposes
  pgm.sql(`
    ALTER TABLE "wallet_ledger_entries" DROP CONSTRAINT IF EXISTS "wallet_ledger_entries_purpose_check";
    ALTER TABLE "wallet_ledger_entries" ADD CONSTRAINT "wallet_ledger_entries_purpose_check"
    CHECK (purpose IN (
      'SETTLEMENT', 'COMMISSION', 'SECURE_PAY_FEE', 'SMS_CHARGE',
      'ESCROW_HOLD', 'ESCROW_RELEASE', 'ESCROW_REFUND',
      'TOPUP', 'WALLET_TOPUP', 'WITHDRAWAL', 'REFERRAL_BONUS', 'REFERRAL_WELCOME',
      'CANCELLATION_PENALTY', 'PENALTY_WAIVER', 'RETURN_FEE', 'RETURN_WAIVER',
      'BULK_DISPATCH', 'COD_COLLECTION',
      'DELIVERY_PAYMENT', 'CANCELLATION_FEE', 'CORPORATE_ORDER',
      'DIRECT_DEBIT_ORDER', 'REFERRAL_REWARD', 'REFEREE_WELCOME',
      'INSURANCE_PREMIUM', 'INSURANCE_CLAIM'
    ));
  `);

  // 4. Seed Settings
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
    ('insurance_rate', '0.01'),
    ('insurance_min_item_value', '10000'),
    ('max_surge_multiplier', '3.0'),
    ('manual_surge_multiplier', '1.0')
    ON CONFLICT (key) DO NOTHING;
  `);
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['surge_multiplier', 'is_insured', 'insurance_fee']);
  pgm.dropColumns('quotes', ['surge_multiplier', 'insurance_fee']);
};
