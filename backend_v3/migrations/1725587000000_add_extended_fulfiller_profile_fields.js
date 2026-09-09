exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    gender: { type: 'varchar(20)' },
    date_of_birth: { type: 'date' },
    home_address: { type: 'text' },
    tier: { type: 'varchar(20)', notNull: true, default: 'Basic' },
    rating_count: { type: 'integer', notNull: true, default: 0 }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('fulfillers', ['gender', 'date_of_birth', 'home_address', 'tier', 'rating_count']);
};
