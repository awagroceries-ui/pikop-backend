const express = require('express');
const router = express.Router();
const expansionController = require('../controllers/expansionController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/waitlist', authenticateToken, expansionController.joinWaitlist);
router.get('/cities/:name/rules', expansionController.getCityRules);

module.exports = router;
