const express = require('express');
const router = express.Router();
const settingsController = require('../controllers/settingsController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/profile', authenticateToken, settingsController.getProfile);
router.patch('/profile', authenticateToken, settingsController.updateProfile);

module.exports = router;
