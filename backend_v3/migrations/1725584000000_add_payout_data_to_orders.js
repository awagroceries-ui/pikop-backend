exports.up = (pgm) => {
  pgm.addColumns('orders', {
    original_delivery_fee: { type: 'decimal(12,2)' },
    original_total_fare: { type: 'decimal(12,2)' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['original_delivery_fee', 'original_total_fare']);
};
