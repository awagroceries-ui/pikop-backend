exports.up = (pgm) => {
  // 1. Add Emergency Contact fields to Fulfillers
  pgm.addColumns('fulfillers', {
    emergency_contact_name: { type: 'varchar(255)' },
    emergency_contact_phone: { type: 'varchar(20)' }
  });

  // 2. Create Emergency Alerts Table
  pgm.createTable('emergency_alerts', {
    id: 'id',
    order_id: { type: 'integer', references: '"orders"', onDelete: 'set null' },
    fulfiller_id: { type: 'integer', notNull: true, references: '"fulfillers"', onDelete: 'cascade' },
    last_location: { type: 'geography(Point, 4326)' },
    status: {
      type: 'varchar(20)',
      notNull: true,
      default: 'OPEN',
      check: "status IN ('OPEN', 'RESOLVED')"
    },
    resolution_notes: { type: 'text' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp')
    }
  });

  pgm.createIndex('emergency_alerts', 'fulfiller_id');
  pgm.createIndex('emergency_alerts', 'status');
};

exports.down = (pgm) => {
  pgm.dropTable('emergency_alerts');
  pgm.dropColumns('fulfillers', ['emergency_contact_name', 'emergency_contact_phone']);
};
