const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// Public Login
router.get('/login', (req, res) => res.render('login', { layout: false }));
router.post('/login', adminController.login);

// Protected Routes
router.get('/dashboard', isAdminAuthenticated, adminController.getDashboard);

// KYC Queue (Ops and Super Admin)
router.get('/kyc', isAdminAuthenticated, hasRole(['ops', 'super_admin']), adminController.getKYCQueue);
router.post('/kyc/:docId/approve', isAdminAuthenticated, hasRole(['ops', 'super_admin']), adminController.approveKYC);

// Placeholders for Zones and Disputes
router.get('/zones', isAdminAuthenticated, hasRole(['ops', 'super_admin']), (req, res) => res.render('error', { message: 'Zone Editor coming soon', admin: req.session.adminUsername }));
router.get('/disputes', isAdminAuthenticated, hasRole(['support', 'super_admin']), (req, res) => res.render('error', { message: 'Dispute Queue coming soon', admin: req.session.adminUsername }));

router.get('/logout', (req, res) => {
  req.session.destroy();
  res.redirect('/admin/login');
});

module.exports = router;
