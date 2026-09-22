exports.up = (pgm) => {
  // Make fulfillers.password_hash nullable as users.password_hash handles authentication credentials
  pgm.alterColumn('fulfillers', 'password_hash', {
    type: 'text',
    notNull: false
  });
};

exports.down = (pgm) => {
  pgm.alterColumn('fulfillers', 'password_hash', {
    type: 'text',
    notNull: true
  });
};
