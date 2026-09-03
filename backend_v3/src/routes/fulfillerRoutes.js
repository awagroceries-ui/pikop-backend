const express = require('express');
const router = express.Router();
const fulfillerController = require('../controllers/fulfillerController');
const { authenticateToken } = require('../middleware/authMiddleware');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadPath = path.join(process.cwd(), 'uploads');
    if (!fs.existsSync(uploadPath)) fs.mkdirSync(uploadPath, { recursive: true });
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    cb(null, `kyc-${Date.now()}${path.extname(file.originalname)}`);
  }
});
const upload = multer({ storage });

router.post('/kyc/start', authenticateToken, fulfillerController.startIdentityVerification);
router.post('/kyc/document', authenticateToken, upload.single('file'), fulfillerController.uploadDocument);
router.post('/kyc/verify-plate', authenticateToken, fulfillerController.verifyVehiclePlate);
router.post('/kyc/webhook', fulfillerController.handleDiditWebhook); // Public for Didit

// Diagnostic
router.get('/ping', (req, res) => res.send('FULFILLER ROUTES ACTIVE'));

// Fleet Operations
router.get('/profile', authenticateToken, fulfillerController.getProfile);
router.patch('/profile', authenticateToken, fulfillerController.updateFulfillerProfile);
router.post('/profile-photo', authenticateToken, upload.single('photo'), fulfillerController.uploadProfilePhoto);
router.patch('/status', authenticateToken, fulfillerController.updateStatus);
router.get('/orders', authenticateToken, fulfillerController.getFulfillerOrders);
router.get('/offers', authenticateToken, fulfillerController.getAvailableOffers);
router.post('/submit-application', authenticateToken, fulfillerController.submitApplication);

module.exports = router;
