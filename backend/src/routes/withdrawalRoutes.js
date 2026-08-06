const express = require('express');
const router = express.Router();
const withdrawalController = require('../controllers/withdrawalController');
const { authenticateToken, requireEmailVerified } = require('../middleware/authMiddleware');

// Request a withdrawal (Requires verified identity/email)
router.post('/', authenticateToken, requireEmailVerified, withdrawalController.requestWithdrawal);

module.exports = router;
