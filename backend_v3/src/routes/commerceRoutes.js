const express = require('express');
const router = express.Router();
const commerceController = require('../controllers/commerceController');
const { authenticateToken } = require('../middleware/authMiddleware');

// Public Browsing (Proximity-aware)
router.get('/discovery', commerceController.getDiscovery);

module.exports = router;
