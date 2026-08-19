const express = require('express');
const router = express.Router();
const addressController = require('../controllers/addressController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/', authenticateToken, addressController.getSavedAddresses);
router.post('/', authenticateToken, addressController.saveAddress);
router.delete('/:id', authenticateToken, addressController.deleteAddress);

module.exports = router;
