const express = require('express');
const router = express.Router();
const kitchenController = require('../controllers/kitchenController');
const { authenticateToken } = require('../middleware/authMiddleware');

// Public Browsing
router.get('/', kitchenController.getKitchens);
router.get('/:id', kitchenController.getKitchenDetails);

// Kitchen Management (Authenticated)
router.post('/register', authenticateToken, kitchenController.registerKitchen);
router.post('/menu-items', authenticateToken, kitchenController.addMenuItem);

module.exports = router;
