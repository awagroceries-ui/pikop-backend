exports.up = (pgm) => {
  // 1. Users Table
  pgm.createTable('users', {
    id: 'id',
    full_name: { type: 'varchar(255)', notNull: true },
    email: { type: 'varchar(255)', notNull: true, unique: true },
    phone: { type: 'varchar(20)', notNull: true, unique: true },
    password_hash: { type: 'text', notNull: true },
    role: {
      type: 'varchar(20)',
      notNull: true,
      default: 'CUSTOMER',
      check: "role IN ('CUSTOMER', 'FULFILLER', 'ADMIN', 'SUPER_ADMIN')"
    },
    referral_code: { type: 'varchar(20)', unique: true },
    referred_by_user_id: {
      type: 'integer',
      references: '"users"',
      onDelete: 'set null',
    },
    email_verified_at: { type: 'timestamp' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
    last_active_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 2. OTP Verifications Table
  pgm.createTable('otp_verifications', {
    id: 'id',
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    otp_code: { type: 'varchar(6)', notNull: true },
    expires_at: { type: 'timestamp', notNull: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 3. User Sessions Table (For Refresh Tokens)
  pgm.createTable('user_sessions', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    refresh_token: { type: 'text', notNull: true },
    device_name: { type: 'varchar(255)' },
    ip_address: { type: 'varchar(45)' },
    is_revoked: { type: 'boolean', notNull: true, default: false },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
    last_active_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  pgm.createIndex('users', 'email');
  pgm.createIndex('users', 'role');
  pgm.createIndex('otp_verifications', 'user_id');
  pgm.createIndex('user_sessions', 'user_id');
};

exports.down = (pgm) => {
  pgm.dropTable('user_sessions');
  pgm.dropTable('otp_verifications');
  pgm.dropTable('users');
};
