const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { otpRateLimiter } = require('../middleware/rateLimiter');

// Public Auth Endpoints
router.post('/signup', otpRateLimiter, authController.signup);
router.post('/login', authController.login);
router.post('/verify-email', authController.verifyEmail);
router.post('/refresh', authController.refreshToken);

module.exports = router;
