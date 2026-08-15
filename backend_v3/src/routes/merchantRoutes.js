const express = require('express');
const router = express.Router();
const merchantController = require('../controllers/merchantController');
const { authenticateToken } = require('../middleware/authMiddleware');
const { authenticateMerchantKey } = require('../middleware/merchantAuth');

// Account Registration (User session)
router.post('/register', authenticateToken, merchantController.registerMerchant);

// Programmatic Bulk Operations (API Key)
router.post('/orders/bulk', authenticateMerchantKey, merchantController.createBulkOrders);
router.get('/batches', authenticateMerchantKey, merchantController.getBatches);

module.exports = router;
