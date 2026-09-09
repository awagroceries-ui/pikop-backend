exports.up = (pgm) => {
  // Indices for Dashboard/Report aggregation
  pgm.createIndex('orders', 'order_type', { ifNotExists: true });
  pgm.createIndex('orders', 'created_at', { ifNotExists: true });
  pgm.createIndex('users', 'role', { ifNotExists: true });
  pgm.createIndex('users', 'last_active_at', { ifNotExists: true });

  // Specific index for revenue calculation
  pgm.createIndex('orders', ['payment_status', 'total_fare'], { ifNotExists: true });
};

exports.down = (pgm) => {
  pgm.dropIndex('orders', 'order_type');
  pgm.dropIndex('orders', 'created_at');
  pgm.dropIndex('users', 'role');
  pgm.dropIndex('users', 'last_active_at');
  pgm.dropIndex('orders', ['payment_status', 'total_fare']);
};
