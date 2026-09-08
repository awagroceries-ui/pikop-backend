exports.up = (pgm) => {
  pgm.addColumns('orders', {
    customer_rating: { type: 'integer' },
    customer_comment: { type: 'text' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['customer_rating', 'customer_comment']);
};
