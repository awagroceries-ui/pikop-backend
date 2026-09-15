exports.up = (pgm) => {
  // 1. Fix Users Role Constraint
  pgm.dropConstraint('users', 'users_role_check');
  pgm.addConstraint('users', 'users_role_check', {
    check: "role IN ('CUSTOMER', 'FULFILLER', 'MERCHANT', 'ADMIN', 'SUPER_ADMIN')"
  });

  // 2. Fix Fulfillers Category Constraint (Make more flexible)
  pgm.dropConstraint('fulfillers', 'fulfillers_primary_class_check');
  pgm.addConstraint('fulfillers', 'fulfillers_primary_class_check', {
    check: "LOWER(primary_class) IN ('agent', 'rider', 'driver')"
  });
};

exports.down = (pgm) => {
  pgm.dropConstraint('users', 'users_role_check');
  pgm.addConstraint('users', 'users_role_check', {
    check: "role IN ('CUSTOMER', 'FULFILLER', 'ADMIN', 'SUPER_ADMIN')"
  });

  pgm.dropConstraint('fulfillers', 'fulfillers_primary_class_check');
  pgm.addConstraint('fulfillers', 'fulfillers_primary_class_check', {
    check: "primary_class IN ('agent', 'rider', 'driver')"
  });
};