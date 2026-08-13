exports.up = (pgm) => {
  // Ensure user_id is unique in fulfillers to support safe auto-creation (ON CONFLICT)
  pgm.addConstraint('fulfillers', 'fulfillers_user_id_unique', {
    unique: ['user_id'],
  });
};

exports.down = (pgm) => {
  pgm.dropConstraint('fulfillers', 'fulfillers_user_id_unique');
};
