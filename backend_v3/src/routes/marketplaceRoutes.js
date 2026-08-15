const express = require('express');
const router = express.Router();
const marketplaceController = require('../controllers/marketplaceController');
const { authenticateToken } = require('../middleware/authMiddleware');

// Public Browsing
router.get('/', marketplaceController.getMarketplace);
router.get('/vendors/:id', marketplaceController.getVendorDetails);

// Vendor Management (Authenticated)
router.post('/vendors/register', authenticateToken, marketplaceController.registerVendor);
router.post('/products', authenticateToken, marketplaceController.addProduct);

module.exports = router;
