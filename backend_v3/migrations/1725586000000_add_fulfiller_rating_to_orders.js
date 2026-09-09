exports.up = (pgm) => {
  pgm.addColumns('orders', {
    fulfiller_rating: { type: 'integer' },
    fulfiller_comment: { type: 'text' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['fulfiller_rating', 'fulfiller_comment']);
};
