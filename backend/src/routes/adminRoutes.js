const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// Public Login
router.get('/login', (req, res) => res.render('login', { layout: false }));
router.post('/login', adminController.login);

// Global Middleware for authenticated admin routes
router.use(isAdminAuthenticated, (req, res, next) => {
  res.locals.admin = req.session.adminUsername;
  res.locals.role = req.session.adminRole;
  next();
});

// Protected Routes
router.get('/dashboard', adminController.getDashboard);

// Analytics and Reports
router.get('/reports/revenue', adminController.getRevenueReport);
router.get('/reports/orders', adminController.getOrderReport);

// Orders and Withdrawals
router.get('/orders', adminController.getOrders);
router.get('/withdrawals', adminController.getWithdrawals);

// Admin Management
router.get('/admins', hasRole(['super_admin']), adminController.getAdmins);
router.post('/admins', hasRole(['super_admin']), adminController.addAdmin);
router.post('/admins/:id/delete', hasRole(['super_admin']), adminController.deleteAdmin);

// Settings
router.get('/settings', hasRole(['super_admin']), adminController.getSettings);
router.post('/settings', hasRole(['super_admin']), adminController.updateSettings);

// KYC Queue (Ops and Super Admin)
router.get('/kyc', hasRole(['ops', 'super_admin']), adminController.getKYCQueue);
router.post('/kyc/:docId/approve', hasRole(['ops', 'super_admin']), adminController.approveKYC);

// Placeholders for Zones and Disputes
router.get('/zones', isAdminAuthenticated, hasRole(['ops', 'super_admin']), (req, res) => res.render('error', { message: 'Zone Editor coming soon', admin: req.session.adminUsername }));
router.get('/disputes', isAdminAuthenticated, hasRole(['support', 'super_admin']), (req, res) => res.render('error', { message: 'Dispute Queue coming soon', admin: req.session.adminUsername }));

router.get('/logout', (req, res) => {
  req.session.destroy();
  res.redirect('/admin/login');
});

module.exports = router;
