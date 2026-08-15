exports.up = (pgm) => {
  // 1. Admin Users Table (Master Brief Milestone 2)
  pgm.createTable('admin_users', {
    id: 'id',
    username: { type: 'varchar(100)', notNull: true, unique: true },
    password_hash: { type: 'text', notNull: true },
    role: {
      type: 'varchar(20)',
      notNull: true,
      default: 'staff',
      check: "role IN ('ops', 'support', 'finance', 'analyst', 'super_admin')"
    },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 2. Settings Table (For Dynamic Pricing/Config)
  pgm.createTable('settings', {
    key: { type: 'varchar(100)', primaryKey: true },
    value: { type: 'text', notNull: true },
    updated_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 3. Audit Logs (Milestone 2)
  pgm.createTable('audit_logs', {
    id: 'id',
    admin_id: { type: 'integer', references: '"admin_users"', onDelete: 'set null' },
    action: { type: 'varchar(255)', notNull: true },
    target_type: { type: 'varchar(50)' },
    target_id: { type: 'varchar(255)' },
    payload: { type: 'jsonb' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
};

exports.down = (pgm) => {
  pgm.dropTable('audit_logs');
  pgm.dropTable('settings');
  pgm.dropTable('admin_users');
};
