exports.up = (pgm) => {
  // 1. Fulfillers Table (Compliant with Master Brief Milestone 2)
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
    secondary_class: { type: 'varchar(20)' },
    mobility_type: {
      type: 'varchar(20)',
      check: "mobility_type IN ('on_foot', 'public_transit', 'bicycle')"
    },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    kyc_status: { type: 'varchar(20)', notNull: true, default: 'NOT_STARTED' },
    didit_session_id: { type: 'varchar(255)' },
    didit_verification_status: { type: 'varchar(50)', default: 'pending' },
    didit_verified_at: { type: 'timestamp' },
    online_status: { type: 'varchar(20)', notNull: true, default: 'OFFLINE' },
    current_location: { type: 'geography(Point, 4326)' },
    last_ping_at: { type: 'timestamp' },
    rating_avg: { type: 'decimal(3,2)', default: 5.0 },
    acceptance_rate: { type: 'decimal(5,2)', default: 0.0 },
    completion_rate: { type: 'decimal(5,2)', default: 0.0 },
    tier: { type: 'varchar(20)', notNull: true, default: 'ROOKIE' },
    wallet_id: { type: 'uuid' },
    bank_account_id: { type: 'uuid' },
    user_id: { type: 'integer', references: '"users"', onDelete: 'set null' },
    last_active_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
    approved_at: { type: 'timestamp' },
  });

  // 2. Addresses Table
  pgm.createTable('addresses', {
    id: 'id',
    user_id: { type: 'integer', references: '"users"', onDelete: 'cascade' },
    label: { type: 'varchar(100)' },
    formatted_address: { type: 'text', notNull: true },
    location: { type: 'geography(Point, 4326)', notNull: true },
    google_place_id: { type: 'varchar(255)' },
    zone_id: { type: 'integer' },
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

  // 4. Unified Order Model (Milestone 2, Item 11)
  pgm.createTable('orders', {
    id: 'id',
    order_type: {
      type: 'varchar(30)',
      notNull: true,
      check: "order_type IN ('pickup_delivery', 'marketplace', 'food')"
    },
    user_id: { type: 'integer', references: '"users"', notNull: true },
    fulfiller_id: { type: 'integer', references: '"fulfillers"' },
    status: { type: 'varchar(30)', notNull: true, default: 'PENDING_PAYMENT' },
    item_description: { type: 'text' },
    item_photo_url: { type: 'text' },
    size_tier: { type: 'varchar(20)' },
    required_classes: { type: 'varchar(20)[]' },
    pickup_address_id: { type: 'integer', references: '"addresses"' },
    delivery_address_id: { type: 'integer', references: '"addresses"' },
    recipient_name: { type: 'varchar(255)' },
    recipient_phone: { type: 'varchar(20)' },
    notes: { type: 'text' },

    // Pricing Breakdown
    base_fare: { type: 'decimal(12,2)', notNull: true, default: 0 },
    distance_fare: { type: 'decimal(12,2)', notNull: true, default: 0 },
    zone_surcharge: { type: 'decimal(12,2)', notNull: true, default: 0 },
    scarcity_surcharge: { type: 'decimal(12,2)', notNull: true, default: 0 },
    traffic_surcharge: { type: 'decimal(12,2)', notNull: true, default: 0 },
    weather_surcharge: { type: 'decimal(12,2)', notNull: true, default: 0 },
    total_fare: { type: 'decimal(12,2)', notNull: true },
    discount_amount: { type: 'decimal(12,2)', notNull: true, default: 0 },
    promo_code_id: { type: 'integer' },
    fare_locked_until: { type: 'timestamp' },

    // Payment & Security
    payment_method: { type: 'varchar(50)' },
    payment_status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    payment_reference: { type: 'varchar(255)', unique: true },
    pickup_code_hash: { type: 'text' },
    delivery_code_hash: { type: 'text' },
    tracking_token: { type: 'uuid', unique: true, default: pgm.func('gen_random_uuid()') },

    // Extensions
    scheduled_at: { type: 'timestamp' },
    batch_id: { type: 'uuid' },
    corporate_account_id: { type: 'uuid' },
    merchant_account_id: { type: 'uuid' },
    vendor_id: { type: 'uuid' },
    kitchen_id: { type: 'uuid' },

    declared_value: { type: 'decimal(12,2)' },
    insurance_opted_in: { type: 'boolean', default: false },
    insurance_fee: { type: 'decimal(12,2)', default: 0 },
    insurance_status: { type: 'varchar(30)' },

    requires_dual_pin: { type: 'boolean', default: false },
    collect_on_delivery_amount: { type: 'decimal(12,2)' },
    collection_status: { type: 'varchar(20)' },

    // Lifecycle Timestamps
    matched_at: { type: 'timestamp' },
    picked_up_at: { type: 'timestamp' },
    delivered_at: { type: 'timestamp' },
    cancelled_at: { type: 'timestamp' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
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
    type: { type: 'varchar(50)', notNull: true },
    plate_number: { type: 'varchar(20)', notNull: true, unique: true },
    verified: { type: 'boolean', notNull: true, default: false },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  pgm.createIndex('fulfillers', 'current_location', { method: 'gist' });
  pgm.createIndex('orders', 'status');
  pgm.createIndex('orders', 'order_type');
  pgm.createIndex('orders', 'tracking_token');
};

exports.down = (pgm) => {
  pgm.dropTable('vehicles');
  pgm.dropTable('orders');
  pgm.dropTable('quotes');
  pgm.dropTable('addresses');
  pgm.dropTable('fulfillers');
};
