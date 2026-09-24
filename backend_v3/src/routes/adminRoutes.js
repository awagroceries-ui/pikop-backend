const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');
const db = require('../config/db');

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

router.use(async (req, res, next) => {
    res.locals.adminUsername = req.session.adminUsername;
    res.locals.role = req.session.adminRole;

    // Fetch Emergency Count for Banner
    try {
        const { rows } = await db.query("SELECT COUNT(*) FROM emergency_alerts WHERE status = 'OPEN'");
        res.locals.emergencyCount = parseInt(rows[0].count);
    } catch (e) {
        res.locals.emergencyCount = 0;
    }
    next();
});

router.get('/dashboard', adminController.getDashboard);
router.get('/orders', adminController.getOrders);
router.get('/orders/:id/track', adminController.trackOrder);
router.post('/orders/:id/update', adminController.updateOrderStatus);

// Fleet & KYC
router.get('/fulfillers', adminController.getFulfillers);
router.get('/fulfillers/:id', adminController.getFulfillerDetail);
router.post('/fulfillers/:id/status', adminController.updateFulfillerStatus);

// Customer Management
router.get('/customers', adminController.getCustomers);
router.get('/customers/:id', adminController.getCustomerDetail);
router.post('/customers/:id/status', adminController.updateCustomerStatus);
router.post('/users/:id/force-delete', adminController.forceDeleteUser);

// Transactions & Ledger Audit
router.get('/transactions', adminController.getTransactions);
router.get('/landmarks', adminController.getLandmarks);
router.get('/traffic', adminController.getTrafficCorridors);
router.post('/traffic', adminController.addTrafficCorridor);

router.get('/kyc', adminController.getKYCQueue);
router.get('/kyc/:id', adminController.getKYCReview);
router.post('/kyc/:id/status', adminController.updateKYCStatus);
router.post('/merchants/:type/:id/status', adminController.updateMerchantKYCStatus);
router.post('/merchants/:type/:id/delete', adminController.deleteMerchant);

// Partners
router.get('/vendors', adminController.getVendors);
router.get('/kitchens', adminController.getKitchens);
router.get('/merchants', adminController.getMerchants);
router.get('/fleet-partners', adminController.getFleetPartners);
router.post('/fleet-partners/:id/status', adminController.updateFleetPartnerStatus);

// Coupons
router.get('/coupons', hasRole(['super_admin']), adminController.getCoupons);
router.post('/coupons', hasRole(['super_admin']), adminController.createCoupon);
router.post('/coupons/:id/delete', hasRole(['super_admin']), adminController.deleteCoupon);

// System Management
router.get('/users', hasRole(['super_admin']), adminController.getAdminUsers);
router.post('/users', hasRole(['super_admin']), adminController.addAdmin);
router.post('/users/:id/delete', hasRole(['super_admin']), adminController.deleteAdmin);
router.get('/profile', adminController.getProfile);

// Support
router.get('/support', adminController.getSupportInbox);
router.get('/support/:id', adminController.getConversationDetails);

// Disputes & Arbitration
router.get('/disputes', adminController.getDisputes);
router.post('/disputes/:id/resolve', adminController.resolveDispute);

// Withdrawals
router.get('/withdrawals', adminController.getWithdrawals);
router.post('/withdrawals/:id/approve', adminController.approveWithdrawal);

// Settings
router.get('/settings', hasRole(['super_admin']), adminController.getSettings);
router.post('/settings', hasRole(['super_admin']), adminController.updateSettings);

// Nationwide Readiness (v4.2)
router.get('/cities', adminController.getCities);
router.post('/cities', adminController.addCity);
router.post('/cities/:id/rules', adminController.updateCityRules);
router.get('/waitlist', adminController.getExpansionWaitlist);

// Emergency SOS (v4.3)
router.get('/emergency', adminController.getEmergencyDashboard);
router.post('/emergency/:id/resolve', adminController.resolveEmergency);

// AI Knowledge Base Management
router.get('/knowledge-base', adminController.getKnowledgeBaseAdmin);
router.post('/knowledge-base', adminController.createKnowledgeArticle);
router.post('/knowledge-base/:id/toggle', adminController.toggleKnowledgeArticle);

// Corporate Accounts
router.get('/corporate', adminController.getCorporateAdmin);
router.post('/corporate/:id/status', adminController.updateCorporateStatus);

// Audit Logs & Compliance Requests
router.get('/audit-logs', adminController.getAuditLogsAdmin);

// Marketplace Returns Dashboard
router.get('/returns', adminController.getReturnsAdmin);

module.exports = router;
