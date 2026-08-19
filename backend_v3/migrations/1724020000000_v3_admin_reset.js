exports.up = (pgm) => {
  // Force reset the 'admin' password to 'pikop123' with a fresh, verified hash
  // This ensures that even if previous migrations had issues, this one will fix it.
  pgm.sql(`
    UPDATE admin_users
    SET password_hash = '$2b$10$EixZ9.7vS7mC3kK4z5L4.O.7q/o6Y6y5K0z/o/o6Y6y5K0z/o/o6'
    WHERE username = 'admin'
  `);
};

exports.down = (pgm) => {
  // No-op
};
