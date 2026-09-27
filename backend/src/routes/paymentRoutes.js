const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');
const { authenticateToken } = require('../middleware/authMiddleware');

// Paystack Webhook (Public)
router.post('/webhook', paymentController.handleWebhook);

// Initialize Payment (Protected)
router.post('/initialize', authenticateToken, paymentController.initializePayment);

// Verify Payment Reference (Protected or Public)
router.get('/verify/:reference', paymentController.verifyPayment);

module.exports = router;
