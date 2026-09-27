const express = require('express');
const router = express.Router();
const walletController = require('../controllers/walletController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/me', authenticateToken, walletController.getWalletInfo);
router.post('/topup', authenticateToken, walletController.initializeTopup);

module.exports = router;
