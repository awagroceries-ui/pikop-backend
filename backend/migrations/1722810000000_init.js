exports.shorthands = undefined;

exports.up = (pgm) => {
  // Users Table
  pgm.createTable('users', {
    id: 'id',
    full_name: { type: 'varchar(255)', notNull: true },
    email: { type: 'varchar(255)', notNull: true, unique: true },
    phone: { type: 'varchar(20)', notNull: true, unique: true },
    password_hash: { type: 'text', notNull: true },
    status: { type: 'varchar(50)', notNull: true, defaultValue: 'ACTIVE' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
    updated_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Fulfillers Table
  pgm.createTable('fulfillers', {
    id: 'id',
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
      onDelete: 'cascade',
    },
    online_status: { type: 'varchar(20)', notNull: true, defaultValue: 'OFFLINE' },
    kyc_status: { type: 'varchar(20)', notNull: true, defaultValue: 'PENDING' },
    location: { type: 'geography(point, 4326)' },
    last_ping_at: { type: 'timestamp' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('fulfillers', 'location', { method: 'gist' });

  // Orders Table
  pgm.createTable('orders', {
    id: 'id',
    user_id: {
      type: 'integer',
      notNull: true,
      references: '"users"',
    },
    fulfiller_id: {
      type: 'integer',
      references: '"fulfillers"',
    },
    pickup_location: { type: 'geography(point, 4326)', notNull: true },
    delivery_location: { type: 'geography(point, 4326)', notNull: true },
    pickup_address: { type: 'text', notNull: true },
    delivery_address: { type: 'text', notNull: true },
    status: { type: 'varchar(50)', notNull: true, defaultValue: 'SEARCHING' },
    total_fare: { type: 'numeric(10, 2)', notNull: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('orders', 'pickup_location', { method: 'gist' });
};

exports.down = (pgm) => {
  pgm.dropTable('orders');
  pgm.dropTable('fulfillers');
  pgm.dropTable('users');
};
