exports.up = (pgm) => {
  pgm.addColumns('orders', {
    payment_reference: { type: 'varchar(255)', unique: true },
    payment_status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    payment_channel: { type: 'varchar(50)' },
    paid_at: { type: 'timestamp' },
  });

  pgm.createIndex('orders', 'payment_reference');
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['payment_reference', 'payment_status', 'payment_channel', 'paid_at']);
};
