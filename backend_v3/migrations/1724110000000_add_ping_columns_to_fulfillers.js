exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    last_ping_at: { type: 'timestamp' },
    last_active_at: { type: 'timestamp' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['last_ping_at', 'last_active_at']);
};
