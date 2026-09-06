const express = require('express');
const router = express.Router();
const merchantController = require('../controllers/merchantController');
const { authenticateToken } = require('../middleware/authMiddleware');
const { authenticateMerchantKey } = require('../middleware/merchantAuth');

// Account Registration (User session)
router.post('/register', authenticateToken, merchantController.registerMerchant);
router.get('/my-batches', authenticateToken, merchantController.getMyBatches);
router.get('/my-batches/:batchId', authenticateToken, merchantController.getBatchStatus);

// Programmatic Bulk Operations (API Key)
router.post('/orders/bulk', authenticateMerchantKey, merchantController.createBulkOrders);
router.get('/batches', authenticateMerchantKey, merchantController.getBatches);
router.get('/batches/:batchId', authenticateMerchantKey, merchantController.getBatchStatus);

module.exports = router;
