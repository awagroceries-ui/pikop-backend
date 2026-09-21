const express = require('express');
const router = express.Router();
const legalController = require('../controllers/legalController');

router.get('/terms', legalController.getTerms);
router.get('/privacy', legalController.getPrivacyPolicy);
router.get('/delete-account', legalController.getDeleteAccountPage);
router.post('/delete-account', legalController.postDeleteAccountRequest);
router.get('/config', legalController.getLegalConfig);

module.exports = router;
