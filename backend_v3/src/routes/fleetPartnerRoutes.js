const express = require('express');
const router = express.Router();
const fleetPartnerController = require('../controllers/fleetPartnerController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/setup', authenticateToken, fleetPartnerController.setupFleetProfile);
router.get('/dashboard', authenticateToken, fleetPartnerController.getFleetDashboard);
router.get('/invite-code', authenticateToken, fleetPartnerController.getInviteCode);

module.exports = router;
