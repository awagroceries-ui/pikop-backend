const express = require('express');
const router = express.Router();
const placesController = require('../controllers/placesController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/autocomplete', authenticateToken, placesController.autocomplete);
router.get('/details', authenticateToken, placesController.details);

module.exports = router;
