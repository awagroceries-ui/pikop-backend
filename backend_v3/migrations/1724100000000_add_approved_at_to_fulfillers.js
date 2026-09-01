exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    approved_at: { type: 'timestamp' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['approved_at']);
};
