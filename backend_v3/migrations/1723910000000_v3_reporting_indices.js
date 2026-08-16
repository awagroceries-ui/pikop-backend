exports.up = (pgm) => {
  // Speed up GMV and volume reports
  pgm.createIndex('orders', 'created_at');
  pgm.createIndex('orders', ['order_type', 'status']);

  // Speed up DAU/MAU lookups
  pgm.createIndex('users', 'last_active_at');
  pgm.createIndex('fulfillers', 'last_active_at');
};

exports.down = (pgm) => {
  pgm.dropIndex('fulfillers', 'last_active_at');
  pgm.dropIndex('users', 'last_active_at');
  pgm.dropIndex('orders', ['order_type', 'status']);
  pgm.dropIndex('orders', 'created_at');
};
