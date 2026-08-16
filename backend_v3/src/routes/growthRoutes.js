const express = require('express');
const router = express.Router();
const growthController = require('../controllers/growthController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/coupons/validate', authenticateToken, growthController.validateCoupon);
router.get('/stats', authenticateToken, growthController.getMyGrowthStats);

module.exports = router;
