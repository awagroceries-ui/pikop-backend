/**
 * Middleware to protect admin routes and enforce RBAC.
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
    res.status(403).render('error', {
      message: 'Access Denied: You do not have permission to perform this action.',
      admin: req.session.adminUsername
    });
  };
};

module.exports = {
  isAdminAuthenticated,
  hasRole
};
