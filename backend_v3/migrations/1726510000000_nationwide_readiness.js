exports.up = (pgm) => {
  // 1. Operating Cities & States Table
  pgm.createTable('operating_cities', {
    id: 'id',
    name: { type: 'varchar(100)', notNull: true, unique: true }, // e.g. Lagos
    state_name: { type: 'varchar(100)', notNull: true }, // e.g. Lagos State
    lat: { type: 'decimal(10,8)', notNull: true },
    lng: { type: 'decimal(11,8)', notNull: true },
    is_active: { type: 'boolean', notNull: true, default: true },
    requires_rider_permit: { type: 'boolean', notNull: true, default: false },
    daylight_start: { type: 'time', notNull: true, default: '06:00' },
    daylight_end: { type: 'time', notNull: true, default: '18:00' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  // 2. Expansion Waitlist (Demand Tracking)
  pgm.createTable('expansion_waitlist', {
    id: 'id',
    user_id: { type: 'integer', references: '"users"', onDelete: 'set null' },
    email: { type: 'varchar(255)', notNull: true },
    city_name: { type: 'varchar(100)', notNull: true },
    state_name: { type: 'varchar(100)' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  // 3. Seed Initial Launch Cities
  pgm.sql(`
    INSERT INTO operating_cities (name, state_name, lat, lng, requires_rider_permit) VALUES
    ('Lagos', 'Lagos State', 6.5244, 3.3792, false),
    ('Abuja', 'FCT', 9.0765, 7.3986, false),
    ('Port Harcourt', 'Rivers State', 4.8156, 7.0498, true)
    ON CONFLICT (name) DO NOTHING;
  `);

  pgm.createIndex('operating_cities', 'is_active');
  pgm.createIndex('expansion_waitlist', 'city_name');
};

exports.down = (pgm) => {
  pgm.dropTable('expansion_waitlist');
  pgm.dropTable('operating_cities');
};
