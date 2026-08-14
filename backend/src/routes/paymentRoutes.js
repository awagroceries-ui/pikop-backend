const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');
const { authenticateToken } = require('../middleware/authMiddleware');

// Paystack Webhook (Public)
router.post('/webhook', paymentController.handleWebhook);

// Initialize Payment (Protected)
router.post('/initialize', authenticateToken, paymentController.initializePayment);

module.exports = router;
