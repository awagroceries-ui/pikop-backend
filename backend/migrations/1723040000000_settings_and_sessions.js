exports.up = (pgm) => {
  // 1. User Sessions Table
  pgm.createTable('user_sessions', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    refresh_token: { type: 'text', notNull: true },
    device_name: { type: 'varchar(255)', nullable: true },
    ip_address: { type: 'varchar(45)', nullable: true },
    last_active: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });
  pgm.createIndex('user_sessions', 'user_id');

  // 2. Saved Recipients Table
  pgm.createTable('saved_recipients', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade' },
    name: { type: 'varchar(255)', notNull: true },
    phone: { type: 'varchar(20)', notNull: true },
    label: { type: 'varchar(50)', nullable: true }, // e.g. 'Mom', 'Wife', 'Shop'
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });
  pgm.createIndex('saved_recipients', 'user_id');

  // 3. Extend Users Table
  pgm.addColumns('users', {
    notification_prefs: { type: 'jsonb', notNull: true, default: JSON.stringify({ push: true, email: true, sms: false }) },
    language: { type: 'varchar(10)', notNull: true, default: 'en' },
    deletion_requested_at: { type: 'timestamp', nullable: true },
  });

  // 4. Extend Fulfillers Table
  pgm.addColumns('fulfillers', {
    preferred_hours: { type: 'jsonb', nullable: true }, // e.g. { 'Mon': ['09:00', '18:00'], ... }
    is_paused: { type: 'boolean', notNull: true, default: false },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['preferred_hours', 'is_paused']);
  pgm.removeColumns('users', ['notification_prefs', 'language', 'deletion_requested_at']);
  pgm.dropTable('saved_recipients');
  pgm.dropTable('user_sessions');
};
