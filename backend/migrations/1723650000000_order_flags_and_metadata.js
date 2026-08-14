exports.up = (pgm) => {
  pgm.addColumns('orders', {
    verification_metadata: { type: 'jsonb', notNull: true, default: '{}' },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['verification_metadata']);
};
