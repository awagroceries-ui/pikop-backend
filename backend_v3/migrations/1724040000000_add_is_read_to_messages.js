exports.up = (pgm) => {
  pgm.addColumns('messages', {
    is_read: { type: 'boolean', notNull: true, default: false }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('messages', ['is_read']);
};
