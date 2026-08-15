const express = require('express');
const router = express.Router();
const supportController = require('../controllers/supportController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.get('/kb', authenticateToken, supportController.getKnowledgeBase);
router.post('/conversations', authenticateToken, supportController.getOrCreateConversation);
router.get('/conversations/:conversationId/messages', authenticateToken, supportController.getMessages);

module.exports = router;
