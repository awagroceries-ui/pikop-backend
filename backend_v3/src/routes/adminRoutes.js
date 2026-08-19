const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// Public
router.get('/login', (req, res) => {
    try {
        res.render('login', { layout: false, error: req.query.error });
    } catch (e) {
        console.error('[Admin] Login Render Error:', e.message);
        res.status(500).send(`Render Error: ${e.message}`);
    }
});
router.post('/login', adminController.login);
router.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/admin/login');
});

// Initial Setup (Only works if zero admins exist)
router.get('/signup', adminController.getSignup);
router.post('/signup', adminController.postSignup);

// Protected
router.use(isAdminAuthenticated);

// Set common locals for all dashboard pages
router.use((req, res, next) => {
    res.locals.adminUsername = req.session.adminUsername;
    res.locals.role = req.session.adminRole;
    next();
});

router.get('/dashboard', adminController.getDashboard);
router.get('/orders', adminController.getOrders);

// Fleet & KYC
router.get('/kyc', adminController.getKYCQueue);

// Partners
router.get('/vendors', adminController.getVendors);
router.get('/kitchens', adminController.getKitchens);
router.get('/merchants', (req, res) => res.render('merchants', { merchants: [] }));
router.get('/coupons', hasRole(['super_admin']), (req, res) => res.render('coupons_admin'));

// System Management
router.get('/users', hasRole(['super_admin']), adminController.getAdminUsers);
router.post('/users', hasRole(['super_admin']), adminController.addAdmin);
router.post('/users/:id/delete', hasRole(['super_admin']), adminController.deleteAdmin);
router.get('/profile', adminController.getProfile);

// Support
router.get('/support', adminController.getSupportInbox);
router.get('/support/:id', adminController.getConversationDetails);

// Settings
router.get('/settings', hasRole(['super_admin']), adminController.getSettings);
router.post('/settings', hasRole(['super_admin']), adminController.updateSettings);

module.exports = router;
