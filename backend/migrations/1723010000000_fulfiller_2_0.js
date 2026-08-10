exports.up = (pgm) => {
  // 1. Extend Fulfillers Table
  pgm.addColumns('fulfillers', {
    mobility_type: {
      type: 'varchar(20)',
      nullable: true
      // on_foot, public_transit, bicycle
    },
    profile_photo_url: { type: 'varchar(255)', nullable: true },
    tier: {
      type: 'varchar(20)',
      notNull: true,
      default: 'bronze'
      // bronze, silver, gold
    },
    primary_class: { type: 'varchar(20)', notNull: true, default: 'agent' },
    secondary_class: { type: 'varchar(20)', nullable: true },
  });

  // 2. Create Vehicles Table
  pgm.createTable('vehicles', {
    id: 'id',
    fulfiller_id: {
      type: 'integer',
      notNull: true,
      references: '"fulfillers"',
      onDelete: 'cascade'
    },
    registration_number: { type: 'varchar(20)', notNull: true, unique: true },
    make: { type: 'varchar(50)', nullable: true },
    model: { type: 'varchar(50)', nullable: true },
    color: { type: 'varchar(20)', nullable: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  pgm.createIndex('vehicles', 'fulfiller_id');
};

exports.down = (pgm) => {
  pgm.dropTable('vehicles');
  pgm.removeColumns('fulfillers', ['mobility_type', 'profile_photo_url', 'tier', 'primary_class', 'secondary_class']);
};
