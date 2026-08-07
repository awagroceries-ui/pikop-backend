const express = require('express');
const router = express.Router();
const walletController = require('../controllers/walletController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/me', authenticateToken, walletController.getWalletInfo);

module.exports = router;
