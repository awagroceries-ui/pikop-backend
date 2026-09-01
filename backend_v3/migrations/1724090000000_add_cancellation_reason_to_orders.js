exports.up = (pgm) => {
  pgm.addColumns('orders', {
    cancellation_reason: { type: 'text' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['cancellation_reason']);
};
