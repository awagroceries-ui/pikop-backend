const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');

// Paystack Webhook (Public)
router.post('/webhook', paymentController.handleWebhook);

module.exports = router;
