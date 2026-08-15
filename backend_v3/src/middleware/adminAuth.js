/**
 * Middleware to protect admin routes.
 */
const isAdminAuthenticated = (req, res, next) => {
  if (req.session && req.session.adminId) {
    return next();
  }
  res.redirect('/admin/login');
};

const hasRole = (roles) => {
  return (req, res, next) => {
    if (req.session && roles.includes(req.session.adminRole)) {
      return next();
    }
    res.status(403).send('Access Denied');
  };
};

module.exports = {
  isAdminAuthenticated,
  hasRole
};
