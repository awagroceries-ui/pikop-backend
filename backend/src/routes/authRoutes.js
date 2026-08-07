const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { otpRateLimiter } = require('../middleware/rateLimiter');

const { authenticateToken } = require('../middleware/authMiddleware');

// Public Auth Endpoints
router.post('/signup', otpRateLimiter, authController.signup);
router.post('/login', authController.login);
router.post('/verify-email', authController.verifyEmail);
router.post('/refresh', authController.refreshToken);

// Protected Auth Endpoints
router.post('/fcm-token', authenticateToken, authController.updateFCMToken);

module.exports = router;
