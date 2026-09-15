exports.up = (pgm) => {
  pgm.createTable('user_legal_consents', {
    id: 'id',
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    terms_version: { type: 'varchar(20)', notNull: true },
    privacy_version: { type: 'varchar(20)', notNull: true },
    consented_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
    ip_address: { type: 'varchar(45)' }
  });

  pgm.createIndex('user_legal_consents', 'user_id');
};

exports.down = (pgm) => {
  pgm.dropTable('user_legal_consents');
};