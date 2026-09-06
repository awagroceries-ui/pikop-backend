exports.up = (pgm) => {
  pgm.addColumns('orders', {
    payer_id: { type: 'integer', references: '"users"', onDelete: 'set null' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['payer_id']);
};
