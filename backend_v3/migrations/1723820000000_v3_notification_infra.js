exports.up = (pgm) => {
  // 1. FCM Tokens
  pgm.createTable('user_fcm_tokens', {
    user_id: {
      type: 'integer',
      primaryKey: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    token: { type: 'text', notNull: true },
    updated_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 2. Notifications Log
  pgm.createTable('notifications', {
    id: 'id',
    user_id: { type: 'integer', references: '"users"' },
    type: { type: 'varchar(50)', notNull: true },
    template: { type: 'varchar(100)' },
    status: { type: 'varchar(20)', default: 'SENT' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
};

exports.down = (pgm) => {
  pgm.dropTable('notifications');
  pgm.dropTable('user_fcm_tokens');
};
