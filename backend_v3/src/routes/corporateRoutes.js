const express = require('express');
const router = express.Router();
const corporateController = require('../controllers/corporateController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/setup', authenticateToken, corporateController.setupCorporateProfile);
router.get('/dashboard', authenticateToken, corporateController.getCorporateDashboard);
router.get('/orders', authenticateToken, corporateController.getCorporateOrders);
router.post('/wallet/topup', authenticateToken, corporateController.initializeCorporateTopup);
router.get('/staff', authenticateToken, corporateController.getStaffMembers);
router.post('/staff', authenticateToken, corporateController.addStaffMember);
router.delete('/staff/:userId', authenticateToken, corporateController.removeStaffMember);
router.get('/my-authorizations', authenticateToken, corporateController.getMyAccounts);

module.exports = router;
