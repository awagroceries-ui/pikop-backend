const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/quote', authenticateToken, orderController.getQuote);

module.exports = router;
