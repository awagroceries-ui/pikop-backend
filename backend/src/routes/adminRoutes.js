const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// --- PUBLIC ADMIN ROUTES ---
router.get('/login', (req, res) => res.render('login', { layout: false }));
router.post('/login', adminController.login);
router.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/admin/login');
});

// --- PROTECTED ADMIN ROUTES ---
// This middleware applies to everything below it
router.use(isAdminAuthenticated);

// Set common locals for all dashboard pages
router.use(async (req, res, next) => {
    res.locals.admin = req.session.adminUsername || 'Admin';
    res.locals.role = req.session.adminRole || 'staff';
    res.locals.adminId = req.session.adminId;
    res.locals.adminUsername = req.session.adminUsername;

    // Global Stats for Header (Safe Fetch)
    try {
        const activeRes = await db.query("SELECT COUNT(*) FROM orders WHERE status IN ('SEARCHING', 'MATCHED', 'PICKED_UP')").catch(() => ({rows:[{count:0}]}));
        const pendingKYC = await db.query("SELECT COUNT(*) FROM fulfillers WHERE kyc_status = 'PENDING_REVIEW'").catch(() => ({rows:[{count:0}]}));
        const openDisputes = await db.query("SELECT COUNT(*) FROM disputes WHERE status = 'OPEN'").catch(() => ({rows:[{count:0}]}));
        const unreadSupport = await db.query("SELECT COUNT(*) FROM conversations WHERE status = 'OPEN'").catch(() => ({rows:[{count:0}]}));

        res.locals.stats = {
            activeOrders: activeRes.rows[0].count,
            notifications: {
                kyc: pendingKYC.rows[0].count,
                disputes: openDisputes.rows[0].count,
                support: unreadSupport.rows[0].count
            }
        };
    } catch (e) {
        res.locals.stats = { activeOrders: 0, notifications: { kyc: 0, disputes: 0, support: 0 } };
    }

    next();
});

// Operational Tabs
router.get('/dashboard', adminController.getDashboard);
router.get('/metrics', hasRole(['ops', 'super_admin', 'analyst']), adminController.getOverviewMetrics);
router.get('/orders', hasRole(['ops', 'super_admin', 'support']), adminController.getOrders);
router.get('/withdrawals', hasRole(['finance', 'ops', 'super_admin']), adminController.getWithdrawals);
router.get('/disputes', hasRole(['support', 'ops', 'super_admin']), adminController.getDisputes);
router.post('/disputes/:id/waive', hasRole(['ops', 'super_admin']), adminController.approveWaiver);
router.post('/disputes/:id/deny', hasRole(['ops', 'super_admin']), adminController.denyWaiver);
router.get('/support', hasRole(['support', 'ops', 'super_admin']), adminController.getSupportInbox);
router.get('/support/:id', hasRole(['support', 'ops', 'super_admin']), adminController.getConversationDetails);
router.post('/support/:id/reply', hasRole(['support', 'ops', 'super_admin']), adminController.replySupport);
router.post('/support/:id/resolve', hasRole(['support', 'ops', 'super_admin']), adminController.resolveSupport);

// Fulfiller Auth (KYC)
router.get('/kyc', hasRole(['ops', 'super_admin']), adminController.getKYCQueue);
router.post('/kyc/:docId/approve', hasRole(['ops', 'super_admin']), adminController.approveKYC);
router.post('/kyc/fulfiller/:id/verify', hasRole(['ops', 'super_admin']), adminController.verifyFulfiller);
router.post('/kyc/fulfiller/:id/approve-identity', hasRole(['ops', 'super_admin']), adminController.forceApproveIdentity);

// Partners & Corporate
router.get('/corporate', hasRole(['ops', 'super_admin']), adminController.getCorporateAccounts);
router.post('/corporate/:id/suspend', hasRole(['ops', 'super_admin']), adminController.suspendCorporateAccount);

// System Management (Super Admin Only)
router.get('/admins', hasRole(['super_admin']), adminController.getAdmins);
router.post('/admins', hasRole(['super_admin']), adminController.addAdmin);
router.post('/admins/:id/delete', hasRole(['super_admin']), adminController.deleteAdmin);

router.get('/settings', hasRole(['super_admin']), adminController.getSettings);
router.post('/settings', hasRole(['super_admin']), adminController.updateSettings);

// Placeholders / coming soon
router.get('/disputes', (req, res) => res.render('error', { message: 'Dispute Board coming soon' }));
router.get('/zones', (req, res) => res.render('error', { message: 'Zone Editor coming soon' }));

// Server Diagnostic Route
router.get('/diagnostics', (req, res) => {
    res.json({
        status: 'UP',
        session: !!req.session,
        adminId: req.session.adminId,
        role: req.session.adminRole,
        authenticated: true
    });
});

module.exports = router;
