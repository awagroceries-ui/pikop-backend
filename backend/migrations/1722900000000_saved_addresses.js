exports.up = (pgm) => {
  pgm.createTable('saved_addresses', {
    id: 'id',
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    label: { type: 'varchar(50)', notNull: true }, // e.g., 'Home', 'Work'
    address_text: { type: 'text', notNull: true },
    location: { type: 'geography(point, 4326)', notNull: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('saved_addresses', 'user_id');
};

exports.down = (pgm) => {
  pgm.dropTable('saved_addresses');
};
