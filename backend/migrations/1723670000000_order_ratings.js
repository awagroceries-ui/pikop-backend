exports.up = (pgm) => {
  pgm.addColumns('orders', {
    rating: { type: 'integer', check: 'rating >= 1 AND rating <= 5', nullable: true },
    rating_comment: { type: 'text', nullable: true }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['rating', 'rating_comment']);
};
