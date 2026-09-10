exports.up = (pgm) => {
  pgm.addColumns('quotes', {
    pickup_landmark: { type: 'text' },
    delivery_landmark: { type: 'text' }
  });
  pgm.addColumns('orders', {
    pickup_landmark: { type: 'text' },
    delivery_landmark: { type: 'text' }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['delivery_landmark', 'pickup_landmark']);
  pgm.dropColumns('quotes', ['delivery_landmark', 'pickup_landmark']);
};
