const express = require('express');
const router = express.Router();
const financialController = require('../controllers/financialController');
const { isAdminAuthenticated, hasRole } = require('../middleware/adminAuth');

// Protected Admin Routes
router.use(isAdminAuthenticated);
router.use(hasRole(['super_admin', 'finance', 'analyst']));

router.get('/', financialController.getFinancialOverview);

module.exports = router;
