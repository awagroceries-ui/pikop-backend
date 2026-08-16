const express = require('express');
const router = express.Router();
const legalController = require('../controllers/legalController');

router.get('/terms', legalController.getTerms);
router.get('/privacy', legalController.getPrivacyPolicy);

module.exports = router;
