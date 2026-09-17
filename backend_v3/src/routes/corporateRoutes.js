const express = require('express');
const router = express.Router();
const corporateController = require('../controllers/corporateController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/setup', authenticateToken, corporateController.setupCorporateProfile);
router.get('/dashboard', authenticateToken, corporateController.getCorporateDashboard);
router.get('/staff', authenticateToken, corporateController.getStaffMembers);
router.post('/staff', authenticateToken, corporateController.addStaffMember);
router.get('/my-authorizations', authenticateToken, corporateController.getMyAccounts);

module.exports = router;
