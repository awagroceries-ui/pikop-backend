const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/initialize', authenticateToken, paymentController.initializePayment);
router.post('/initialize/cod/:orderId', authenticateToken, paymentController.initializeCoDPayment);
router.get('/verify/:reference', authenticateToken, paymentController.verifyPayment);
router.post('/webhook', paymentController.handleWebhook);
router.get('/webhook', paymentController.handleWebhookGET);

module.exports = router;
