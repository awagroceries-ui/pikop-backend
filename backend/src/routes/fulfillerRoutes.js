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
    cb(null, `kyc-${Date.now()}${path.extname(file.originalname)}`);
  }
});
const upload = multer({ storage });

router.patch('/status', authenticateToken, fulfillerController.updateStatus);
router.get('/offers', authenticateToken, fulfillerController.getOffers);
router.get('/orders', authenticateToken, fulfillerController.getFulfillerOrders);
router.post('/kyc', authenticateToken, upload.single('document'), fulfillerController.uploadKYC);

module.exports = router;
