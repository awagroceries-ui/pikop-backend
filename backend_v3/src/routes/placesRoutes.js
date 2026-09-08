const express = require('express');
const router = express.Router();
const placesController = require('../controllers/placesController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/autocomplete', placesController.autocomplete);
router.get('/details', placesController.details);

module.exports = router;
