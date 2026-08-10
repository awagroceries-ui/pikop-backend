exports.up = (pgm) => {
  // 1. Promo Codes Table
  pgm.createTable('promo_codes', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    code: { type: 'varchar(50)', notNull: true, unique: true },
    discount_type: { type: 'varchar(20)', notNull: true }, // 'flat', 'percentage'
    value: { type: 'numeric(12, 2)', notNull: true },
    max_uses: { type: 'integer', notNull: true, default: 100 },
    used_count: { type: 'integer', notNull: true, default: 0 },
    valid_from: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
    valid_to: { type: 'timestamp', notNull: true },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });

  // 2. Promo Code Redemptions (One per user per code)
  pgm.createTable('promo_code_redemptions', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    promo_code_id: { type: 'uuid', notNull: true, references: '"promo_codes"', onDelete: 'cascade' },
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    order_id: { type: 'integer', notNull: true, references: '"orders"', onDelete: 'cascade' },
    redeemed_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });
  pgm.createIndex('promo_code_redemptions', ['promo_code_id', 'user_id'], { unique: true });

  // 3. Referral Rewards Table
  pgm.createTable('referral_rewards', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    referrer_user_id: { type: 'integer', notNull: true, references: '"users"' },
    referee_user_id: { type: 'integer', notNull: true, references: '"users"' },
    reward_amount: { type: 'numeric(12, 2)', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' }, // 'pending', 'credited'
    triggered_by_order_id: { type: 'integer', references: '"orders"' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });

  // 4. Update Users Table for Referrals
  pgm.addColumns('users', {
    referral_code: { type: 'varchar(20)', unique: true },
    referred_by_user_id: { type: 'integer', references: '"users"' },
  });

  // 5. Update Orders Table for Tracking and Promos
  pgm.addColumns('orders', {
    tracking_token: { type: 'uuid', unique: true, default: pgm.func('gen_random_uuid()') },
    promo_code_id: { type: 'uuid', references: '"promo_codes"', onDelete: 'set null' },
    discount_amount: { type: 'numeric(12, 2)', notNull: true, default: 0 },
  });

  pgm.createIndex('orders', 'tracking_token');
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['tracking_token', 'promo_code_id', 'discount_amount']);
  pgm.removeColumns('users', ['referral_code', 'referred_by_user_id']);
  pgm.dropTable('referral_rewards');
  pgm.dropTable('promo_code_redemptions');
  pgm.dropTable('promo_codes');
};
