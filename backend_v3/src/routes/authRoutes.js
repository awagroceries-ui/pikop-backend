const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/signup', authController.signup);
router.post('/login', authController.login);
router.post('/verify-email', authController.verifyEmail);
router.post('/resend-otp', authController.resendOtp);
router.post('/refresh', authController.refresh);
router.post('/fcm-token', authenticateToken, authController.updateFCMToken);
router.post('/change-password', authenticateToken, authController.changePassword);
router.post('/delete-account', authenticateToken, authController.deleteAccount);

module.exports = router;
