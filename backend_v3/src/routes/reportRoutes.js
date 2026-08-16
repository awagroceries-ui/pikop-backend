const express = require('express');
const router = express.Router();
const reportController = require('../controllers/reportController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// Protected Admin Routes
router.use(isAdminAuthenticated);

// Analytics API
router.get('/api/performance', reportController.getModulePerformance);
router.get('/api/users', reportController.getUserAnalytics);

// Views
router.get('/', hasRole(['super_admin', 'analyst']), reportController.renderReports);

// Exports
router.get('/export/missions', hasRole(['super_admin', 'finance']), reportController.exportMissions);

module.exports = router;
