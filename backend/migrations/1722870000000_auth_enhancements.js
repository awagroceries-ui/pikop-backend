exports.up = (pgm) => {
  // Add email_verified_at to users
  pgm.addColumns('users', {
    email_verified_at: { type: 'timestamp' },
  });

  // OTP Verifications Table
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
};

exports.down = (pgm) => {
  pgm.dropTable('otp_verifications');
  pgm.removeColumns('users', ['email_verified_at']);
};
