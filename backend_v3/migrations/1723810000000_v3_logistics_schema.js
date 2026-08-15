exports.up = (pgm) => {
  // 1. Fulfillers Table
  pgm.createTable('fulfillers', {
    id: 'id',
    full_name: { type: 'varchar(255)', notNull: true },
    email: { type: 'varchar(255)', notNull: true, unique: true },
    email_verified_at: { type: 'timestamp' },
    phone: { type: 'varchar(20)', notNull: true, unique: true },
    password_hash: { type: 'text', notNull: true },
    primary_class: {
      type: 'varchar(20)',
      notNull: true,
      check: "primary_class IN ('agent', 'rider', 'driver')"
    },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    kyc_status: { type: 'varchar(20)', notNull: true, default: 'NOT_STARTED' },
    didit_session_id: { type: 'varchar(255)' },
    didit_verification_status: { type: 'varchar(50)', default: 'pending' },
    online_status: { type: 'varchar(20)', notNull: true, default: 'OFFLINE' },
    current_location: { type: 'geography(Point, 4326)' },
    rating_avg: { type: 'decimal(3,2)', default: 5.0 },
    user_id: { type: 'integer', references: '"users"', onDelete: 'set null' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });

  // 2. Addresses Table (CRITICAL: Used as foreign key in v3_business_entities)
  pgm.createTable('addresses', {
    id: 'id',
    user_id: { type: 'integer', references: '"users"', onDelete: 'cascade' },
    label: { type: 'varchar(100)' },
    formatted_address: { type: 'text', notNull: true },
    location: { type: 'geography(Point, 4326)', notNull: true },
    landmark_description: { type: 'text', notNull: true },
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

  // 4. Unified Orders Table (Milestone 2)
  pgm.createTable('orders', {
    id: 'id',
    order_type: {
      type: 'varchar(30)',
      notNull: true,
      check: "order_type IN ('pickup_delivery', 'marketplace', 'food')"
    },
    user_id: { type: 'integer', references: '"users"', notNull: true },
    fulfiller_id: { type: 'integer', references: '"fulfillers"' },
    status: { type: 'varchar(30)', notNull: true, default: 'SEARCHING' },
    pickup_address: { type: 'text', notNull: true },
    delivery_address: { type: 'text', notNull: true },
    pickup_location: { type: 'geography(Point, 4326)', notNull: true },
    delivery_location: { type: 'geography(Point, 4326)', notNull: true },
    total_fare: { type: 'decimal(12,2)', notNull: true },
    payment_reference: { type: 'varchar(255)', unique: true },
    payment_status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    tracking_token: { type: 'uuid', unique: true, default: pgm.func('gen_random_uuid()') },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // 5. Vehicles Table
  pgm.createTable('vehicles', {
    id: 'id',
    fulfiller_id: {
      type: 'integer',
      notNull: true,
      references: '"fulfillers"',
      onDelete: 'cascade',
    },
    plate_number: { type: 'varchar(20)', notNull: true, unique: true },
    type: { type: 'varchar(50)', notNull: true },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  pgm.createIndex('fulfillers', 'current_location', { method: 'gist' });
  pgm.createIndex('orders', 'status');
};

exports.down = (pgm) => {
  pgm.dropTable('vehicles');
  pgm.dropTable('orders');
  pgm.dropTable('quotes');
  pgm.dropTable('addresses');
  pgm.dropTable('fulfillers');
};
