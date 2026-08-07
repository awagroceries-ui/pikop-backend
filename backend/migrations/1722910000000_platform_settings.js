exports.up = (pgm) => {
  pgm.createTable('settings', {
    key: { type: 'varchar(100)', primaryKey: true },
    value: { type: 'varchar(255)', notNull: true },
    updated_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Seed default commission (25%)
  pgm.sql("INSERT INTO settings (key, value) VALUES ('platform_commission', '0.25')");
};

exports.down = (pgm) => {
  pgm.dropTable('settings');
};
