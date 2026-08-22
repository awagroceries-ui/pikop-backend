exports.up = (pgm) => {
  pgm.addColumns('orders', {
    quote_id: { type: 'uuid', references: '"quotes"', onDelete: 'set null' }
  });
  pgm.createIndex('orders', 'quote_id');
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['quote_id']);
};
