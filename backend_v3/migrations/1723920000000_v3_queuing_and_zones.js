exports.up = (pgm) => {
  // 1. Zones Table
  pgm.createTable('zones', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    name: { type: 'varchar(100)', notNull: true },
    boundary: { type: 'geography(Polygon, 4326)', notNull: true },
    allowed_fulfiller_classes: {
      type: 'text[]',
      notNull: true,
      default: pgm.func("ARRAY['agent', 'rider', 'driver']::text[]")
    },
    flood_prone: { type: 'boolean', notNull: true, default: false },
    weather_mode: { type: 'varchar(20)', notNull: true, default: 'auto' },
    is_dispatch_paused: { type: 'boolean', notNull: true, default: false },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 2. Queuing and Return Infrastructure for Orders
  pgm.addColumns('orders', {
    queued_for_fulfiller_id: { type: 'integer', references: '"fulfillers"', onDelete: 'set null' },
    parent_order_id: { type: 'integer', references: '"orders"', onDelete: 'set null' },
    return_fare_paid: { type: 'boolean', default: false },
    matched_at: { type: 'timestamp' },
    picked_up_at: { type: 'timestamp' },
    delivered_at: { type: 'timestamp' },
  });

  // 3. Landmark Suggestions (Crowdsourced)
  pgm.createTable('landmark_suggestions', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    normalized_text: { type: 'varchar(255)', notNull: true },
    display_text: { type: 'varchar(255)', notNull: true },
    location: { type: 'geography(Point, 4326)', notNull: true },
    submission_count: { type: 'integer', default: 1 },
    status: { type: 'varchar(20)', default: 'approved' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  pgm.createIndex('zones', 'boundary', { method: 'gist' });
  pgm.createIndex('landmark_suggestions', 'normalized_text');
  pgm.createIndex('orders', 'queued_for_fulfiller_id');
};

exports.down = (pgm) => {
  pgm.dropTable('landmark_suggestions');
  pgm.removeColumns('orders', ['delivered_at', 'picked_up_at', 'matched_at', 'return_fare_paid', 'parent_order_id', 'queued_for_fulfiller_id']);
  pgm.dropTable('zones');
};
