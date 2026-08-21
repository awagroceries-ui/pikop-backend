const express = require('express');
const router = express.Router();
const webhookController = require('../controllers/webhookController');

router.post('/prembly', webhookController.handlePremblyWebhook);
router.get('/redirect', webhookController.handlePremblyRedirect);

module.exports = router;
