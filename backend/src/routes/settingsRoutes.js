const express = require('express');
const router = express.Router();
const settingsController = require('../controllers/settingsController');
const { authenticateToken } = require('../middleware/authMiddleware');

router.use(authenticateToken);

router.get('/profile', settingsController.getProfile);
router.patch('/profile', settingsController.updateProfile);
router.patch('/change-password', settingsController.changePassword);
router.patch('/notifications', settingsController.updateNotificationPrefs);
router.post('/fulfiller/toggle-pause', settingsController.toggleFulfillerPause);

router.get('/recipients', settingsController.getRecipients);
router.post('/recipients', settingsController.addRecipient);
router.delete('/recipients/:id', settingsController.deleteRecipient);

router.get('/sessions', settingsController.getSessions);
router.delete('/sessions/:id', settingsController.revokeSession);

router.post('/delete-request', settingsController.requestDeletion);

module.exports = router;
