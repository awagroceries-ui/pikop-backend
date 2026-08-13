const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// Public Login
router.get('/login', (req, res) => res.render('login', { layout: false }));
router.post('/login', adminController.login);
router.get('/logout', (req, res) => {
  req.session.destroy();
  res.redirect('/admin/login');
});

// Global Middleware for authenticated admin routes
router.use(isAdminAuthenticated, (req, res, next) => {
  res.locals.admin = req.session.adminUsername || 'Admin';
  res.locals.role = req.session.adminRole || 'staff';
  res.locals.adminId = req.session.adminId;
  next();
});

// Dashboard & Overview
router.get('/dashboard', adminController.getDashboard);

// Analytics and Reports
router.get('/reports/revenue', hasRole(['ops', 'super_admin', 'analyst']), adminController.getRevenueReport);
router.get('/reports/orders', hasRole(['ops', 'super_admin', 'analyst']), adminController.getOrderReport);

// Core Operational Tabs
router.get('/orders', hasRole(['ops', 'super_admin', 'support']), adminController.getOrders);
router.get('/withdrawals', hasRole(['finance', 'ops', 'super_admin']), adminController.getWithdrawals);
router.get('/disputes', hasRole(['support', 'ops', 'super_admin']), adminController.getDisputes);

// Support Inbox
router.get('/support', hasRole(['support', 'ops', 'super_admin']), adminController.getSupportInbox);
router.get('/support/:id', hasRole(['support', 'ops', 'super_admin']), adminController.getConversationDetails);
router.post('/support/:id/resolve', hasRole(['support', 'ops', 'super_admin']), adminController.resolveSupport);

// KYC Queue
router.get('/kyc', hasRole(['ops', 'super_admin']), adminController.getKYCQueue);
router.post('/kyc/:docId/approve', hasRole(['ops', 'super_admin']), adminController.approveKYC);
router.post('/kyc/fulfiller/:id/verify', hasRole(['ops', 'super_admin']), adminController.verifyFulfiller);
router.post('/kyc/fulfiller/:id/approve-identity', hasRole(['ops', 'super_admin']), adminController.forceApproveIdentity);

// Corporate Accounts
router.get('/corporate', hasRole(['ops', 'super_admin']), adminController.getCorporateAccounts);
router.post('/corporate/:id/suspend', hasRole(['ops', 'super_admin']), adminController.suspendCorporateAccount);

// Admin Management
router.get('/admins', hasRole(['super_admin']), adminController.getAdmins);
router.post('/admins', hasRole(['super_admin']), adminController.addAdmin);
router.post('/admins/:id/delete', hasRole(['super_admin']), adminController.deleteAdmin);

// Settings
router.get('/settings', hasRole(['super_admin']), adminController.getSettings);
router.post('/settings', hasRole(['super_admin']), adminController.updateSettings);

// Zone Editor (Placeholder)
router.get('/zones', hasRole(['ops', 'super_admin']), (req, res) => res.render('error', { message: 'Zone Editor coming soon', admin: req.session.adminUsername }));

module.exports = router;
