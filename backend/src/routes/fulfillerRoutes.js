const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fulfillerController = require('../controllers/fulfillerController');
const { authenticateToken } = require('../middleware/authMiddleware');

// Setup Multer for disk storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, 'uploads/');
  },
  filename: (req, file, cb) => {
    const prefix = file.fieldname === 'photo' ? 'profile' : 'kyc';
    cb(null, `${prefix}-${Date.now()}${path.extname(file.originalname)}`);
  }
});
const upload = multer({ storage });

router.patch('/status', authenticateToken, fulfillerController.updateStatus);
router.get('/profile', authenticateToken, fulfillerController.getProfile);
router.patch('/profile', authenticateToken, fulfillerController.updateProfile);
router.post('/profile-photo', authenticateToken, upload.single('photo'), fulfillerController.uploadProfilePhoto);
router.get('/offers', authenticateToken, fulfillerController.getOffers);
router.get('/orders', authenticateToken, fulfillerController.getFulfillerOrders);
router.post('/kyc', authenticateToken, upload.single('document'), fulfillerController.uploadKYC);
router.post('/submit-application', authenticateToken, fulfillerController.submitApplication);
router.post('/kyc/start-verification', authenticateToken, fulfillerController.startDiditVerification);
router.post('/kyc/webhook', fulfillerController.handleDiditWebhook); // Correct webhook
router.post('/kyc/done', fulfillerController.handleDiditWebhook); // Temporary alias for existing sessions

module.exports = router;
