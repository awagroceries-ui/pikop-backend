const express = require('express');
const router = express.Router();
const growthController = require('../controllers/growthController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/coupons/validate', authenticateToken, growthController.validateCoupon);
router.post('/validate', authenticateToken, growthController.validateCoupon); // Legacy Bridge
router.get('/stats', authenticateToken, growthController.getMyGrowthStats);
router.post('/redeem', authenticateToken, growthController.redeemPoints);
router.get('/referrals', authenticateToken, growthController.getReferralHistory);

module.exports = router;
