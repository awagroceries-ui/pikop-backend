exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    last_active_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['last_active_at']);
};
