exports.up = (pgm) => {
  // 1. Traffic Corridors (Static Rush-Hour Multipliers)
  pgm.createTable('traffic_corridors', {
    id: 'id',
    name: { type: 'varchar(100)', notNull: true },
    pickup_zone_id: { type: 'uuid', references: '"zones"', onDelete: 'cascade' },
    delivery_zone_id: { type: 'uuid', references: '"zones"', onDelete: 'cascade' },
    time_windows: {
      type: 'jsonb',
      notNull: true,
      default: pgm.func("'[]'::jsonb") // Array of {day_of_week, start_time, end_time, multiplier}
    },
    is_active: { type: 'boolean', notNull: true, default: true },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') }
  });

  // 2. Weather Event Logs
  pgm.createTable('weather_events', {
    id: 'id',
    city: { type: 'varchar(50)', notNull: true },
    alert_type: { type: 'varchar(100)', notNull: true }, // severe_storm, heavy_rain, clear
    applied_multiplier: { type: 'decimal(4,2)', notNull: true, default: 1.0 },
    source_payload: { type: 'jsonb' },
    resolved_at: { type: 'timestamp' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') }
  });

  // 3. Extend Zones for Weather surcharges
  pgm.addColumns('zones', {
    weather_multiplier: { type: 'decimal(4,2)', notNull: true, default: 1.0 }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('zones', ['weather_multiplier']);
  pgm.dropTable('weather_events');
  pgm.dropTable('traffic_corridors');
};
