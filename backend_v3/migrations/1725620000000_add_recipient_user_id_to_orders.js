exports.up = (pgm) => {
  pgm.addColumns('orders', {
    recipient_user_id: { type: 'integer', references: '"users"', onDelete: 'set null' }
  });
  pgm.createIndex('orders', 'recipient_user_id');
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['recipient_user_id']);
};
