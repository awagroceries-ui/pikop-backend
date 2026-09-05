exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    account_name: { type: 'varchar(255)' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['account_name']);
};
