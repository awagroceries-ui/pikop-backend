exports.up = (pgm) => {
  // 1. Extend Users for simple loyalty tracking
  pgm.addColumns('users', {
    total_orders_completed: { type: 'integer', notNull: true, default: 0 }
  });

  // 2. Extend Fulfillers for Streak tracking
  pgm.addColumns('fulfillers', {
    current_streak_days: { type: 'integer', notNull: true, default: 0 },
    last_streak_date: { type: 'date' }
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
      'INSURANCE_PREMIUM', 'INSURANCE_CLAIM',
      'STREAK_BONUS', 'PEAK_BONUS'
    ));
  `);

  // 4. Seed Settings
  pgm.sql(`
    INSERT INTO settings (key, value) VALUES
    ('streak_bonus_7_day', '1000'),
    ('streak_bonus_30_day', '5000'),
    ('peak_hour_start', '16:00'),
    ('peak_hour_end', '19:00'),
    ('peak_hour_bonus', '300')
    ON CONFLICT (key) DO NOTHING;
  `);
};

exports.down = (pgm) => {
  pgm.dropColumns('fulfillers', ['current_streak_days', 'last_streak_date']);
  pgm.dropColumns('users', ['total_orders_completed']);
};
