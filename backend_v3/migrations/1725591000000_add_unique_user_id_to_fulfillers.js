exports.up = (pgm) => {
  pgm.addConstraint('fulfillers', 'fulfillers_user_id_unique', {
    unique: 'user_id'
  });
};

exports.down = (pgm) => {
  pgm.dropConstraint('fulfillers', 'fulfillers_user_id_unique');
};
