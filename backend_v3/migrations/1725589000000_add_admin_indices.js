exports.up = (pgm) => {
  pgm.createIndex('orders', 'payment_status');
  pgm.createIndex('orders', 'user_id');
  pgm.createIndex('orders', 'fulfiller_id');
  pgm.createIndex('fulfillers', 'online_status');
  pgm.createIndex('fulfillers', 'kyc_status');
};

exports.down = (pgm) => {
  pgm.dropIndex('orders', 'payment_status');
  pgm.dropIndex('orders', 'user_id');
  pgm.dropIndex('orders', 'fulfiller_id');
  pgm.dropIndex('fulfillers', 'online_status');
  pgm.dropIndex('fulfillers', 'kyc_status');
};
