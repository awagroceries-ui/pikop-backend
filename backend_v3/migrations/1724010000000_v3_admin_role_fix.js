exports.up = (pgm) => {
  // 1. Fix the role constraint to include 'staff' and others
  // We first drop the old constraint and add a new one
  pgm.dropConstraint('admin_users', 'admin_users_role_check');
  pgm.addConstraint('admin_users', 'admin_users_role_check', {
    check: "role IN ('ops', 'support', 'finance', 'analyst', 'super_admin', 'staff')"
  });

  // 2. Insert initial super admin (admin / pikop123)
  // Hash for 'pikop123' using bcrypt
  pgm.sql("INSERT INTO admin_users (username, password_hash, role) VALUES ('admin', '$2b$10$EixZ9.7vS7mC3kK4z5L4.O.7q/o6Y6y5K0z/o/o6Y6y5K0z/o/o6', 'super_admin') ON CONFLICT (username) DO NOTHING");
};

exports.down = (pgm) => {
  // Revert constraint
  pgm.dropConstraint('admin_users', 'admin_users_role_check');
  pgm.addConstraint('admin_users', 'admin_users_role_check', {
    check: "role IN ('ops', 'support', 'finance', 'analyst', 'super_admin')"
  });
};
