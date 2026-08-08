exports.up = (pgm) => {
  pgm.addColumns('users', {
    role: { type: 'varchar(20)', notNull: true, default: 'CUSTOMER' },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('users', ['role']);
};
