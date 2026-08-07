exports.up = (pgm) => {
  pgm.createTable('fcm_tokens', {
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade', primaryKey: true },
    token: { type: 'text', notNull: true },
    updated_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });
};

exports.down = (pgm) => {
  pgm.dropTable('fcm_tokens');
};
