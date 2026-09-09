exports.up = (pgm) => {
  pgm.addColumns('quotes', {
    pickup_state: { type: 'varchar(100)' }
  });
  pgm.addColumns('orders', {
    pickup_state: { type: 'varchar(100)' }
  });
  pgm.addColumns('fulfillers', {
    current_state: { type: 'varchar(100)' }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('quotes', ['pickup_state']);
  pgm.dropColumns('orders', ['pickup_state']);
  pgm.dropColumns('fulfillers', ['current_state']);
};
