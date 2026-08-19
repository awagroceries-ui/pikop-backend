exports.up = (pgm) => {
  pgm.createTable('saved_addresses', {
    id: 'id',
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    label: { type: 'varchar(50)', notNull: true }, // Home, Work, Custom
    address_text: { type: 'text', notNull: true },
    location: { type: 'geometry(Point, 4326)', notNull: true },
    landmark: { type: 'text' },
    place_id: { type: 'varchar(255)' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
    last_used_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Ensure unique label per user
  pgm.addConstraint('saved_addresses', 'unique_user_label', {
    unique: ['user_id', 'label'],
  });

  pgm.createIndex('saved_addresses', 'user_id');
  pgm.createIndex('saved_addresses', 'location', { method: 'gist' });
};

exports.down = (pgm) => {
  pgm.dropTable('saved_addresses');
};
