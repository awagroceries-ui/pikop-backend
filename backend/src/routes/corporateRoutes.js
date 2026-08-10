const express = require('express');
const router = express.Router();
const corporateController = require('../controllers/corporateController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.post('/accounts', authenticateToken, corporateController.createAccount);
router.get('/my-accounts', authenticateToken, corporateController.getMyAccounts);
router.post('/accounts/:id/mandate/authorize', authenticateToken, corporateController.authorizeMandate);
router.post('/accounts/:id/sub-accounts', authenticateToken, corporateController.addStaff);
router.get('/accounts/:id/sub-accounts', authenticateToken, corporateController.getStaff);

module.exports = router;
