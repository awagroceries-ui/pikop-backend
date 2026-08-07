const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
const { authenticateToken, requireEmailVerified } = require('../middleware/authMiddleware');

// Quoting (Available to all authenticated users)
router.post('/quote', authenticateToken, orderController.getQuote);

// Order Lifecycle (Requires verified email)
router.get('/:orderId', authenticateToken, orderController.getOrderDetails);
router.post('/', authenticateToken, requireEmailVerified, orderController.createOrder);
router.post('/:orderId/accept', authenticateToken, orderController.acceptOrder);
router.post('/:orderId/pickup', authenticateToken, orderController.verifyPickup);
router.post('/:orderId/deliver', authenticateToken, orderController.verifyDelivery);

module.exports = router;
