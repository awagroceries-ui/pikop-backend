const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/initialize', authenticateToken, paymentController.initializePayment);
router.post('/webhook', paymentController.handleWebhook);

module.exports = router;
