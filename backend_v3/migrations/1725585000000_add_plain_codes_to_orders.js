exports.up = (pgm) => {
  pgm.addColumns('orders', {
    pickup_code: { type: 'varchar(10)' },
    delivery_code: { type: 'varchar(10)' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['pickup_code', 'delivery_code']);
};
