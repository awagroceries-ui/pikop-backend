exports.up = (pgm) => {
  // 1. Fulfillers Table
  pgm.createTable('fulfillers', {
    id: 'id',
    user_id: {
      type: 'integer',
      notNull: true,
      unique: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    primary_class: { type: 'varchar(20)' }, // agent, rider, driver
    kyc_status: {
      type: 'varchar(20)',
      notNull: true,
      default: 'NOT_STARTED'
    },
    didit_session_id: { type: 'varchar(255)' },
    didit_verification_status: { type: 'varchar(50)', default: 'pending' },
    online_status: { type: 'varchar(20)', default: 'OFFLINE' },
    location: { type: 'geography(Point, 4326)' },
    profile_photo_url: { type: 'text' },
    tier: { type: 'varchar(20)', default: 'ROOKIE' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 2. Vehicles Table
  pgm.createTable('vehicles', {
    id: 'id',
    fulfiller_id: {
      type: 'integer',
      notNull: true,
      references: '"fulfillers"',
      onDelete: 'cascade',
    },
    registration_number: { type: 'varchar(50)', unique: true, notNull: true },
    make: { type: 'varchar(100)' },
    model: { type: 'varchar(100)' },
    color: { type: 'varchar(50)' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 3. Quotes Table
  pgm.createTable('quotes', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    user_id: { type: 'integer', references: '"users"' },
    pickup_address: { type: 'text', notNull: true },
    delivery_address: { type: 'text', notNull: true },
    pickup_location: { type: 'geography(Point, 4326)', notNull: true },
    delivery_location: { type: 'geography(Point, 4326)', notNull: true },
    item_description: { type: 'text' },
    size_tier: { type: 'varchar(20)' },
    total_fare: { type: 'decimal(12,2)', notNull: true },
    expires_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func("current_timestamp + interval '15 minutes'")
    },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 4. Orders Table
  pgm.createTable('orders', {
    id: 'id',
    user_id: { type: 'integer', references: '"users"', notNull: true },
    fulfiller_id: { type: 'integer', references: '"fulfillers"' },
    quote_id: { type: 'uuid', references: '"quotes"' },
    status: {
      type: 'varchar(30)',
      notNull: true,
      default: 'SEARCHING'
    },
    pickup_address: { type: 'text', notNull: true },
    delivery_address: { type: 'text', notNull: true },
    pickup_location: { type: 'geography(Point, 4326)', notNull: true },
    delivery_location: { type: 'geography(Point, 4326)', notNull: true },
    total_fare: { type: 'decimal(12,2)', notNull: true },
    tracking_token: { type: 'uuid', unique: true, default: pgm.func('gen_random_uuid()') },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  pgm.createIndex('fulfillers', 'location', { method: 'gist' });
  pgm.createIndex('orders', 'status');
};

exports.down = (pgm) => {
  pgm.dropTable('orders');
  pgm.dropTable('quotes');
  pgm.dropTable('vehicles');
  pgm.dropTable('fulfillers');
};
