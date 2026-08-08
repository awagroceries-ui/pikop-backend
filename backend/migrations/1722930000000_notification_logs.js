exports.up = (pgm) => {
  pgm.createTable('notification_logs', {
    id: 'id',
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    channel: { type: 'varchar(20)', notNull: true }, // e.g., 'email', 'push'
    template_name: { type: 'varchar(100)', notNull: true },
    recipient: { type: 'varchar(255)', notNull: true },
    status: { type: 'varchar(20)', notNull: true }, // 'SUCCESS', 'FAILED'
    error_message: { type: 'text' },
    sent_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('notification_logs', ['user_id', 'template_name']);
};

exports.down = (pgm) => {
  pgm.dropTable('notification_logs');
};
