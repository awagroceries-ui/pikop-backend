const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
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
    cb(null, `order-${Date.now()}${path.extname(file.originalname)}`);
  }
});
const upload = multer({ storage });

router.post('/quote', authenticateToken, orderController.getQuote);
router.get('/by-quote/:quoteId', authenticateToken, orderController.getOrderByQuote);
router.get('/', authenticateToken, orderController.getUserOrders);
router.post('/', authenticateToken, orderController.createOrder);
router.get('/:orderId', authenticateToken, orderController.getOrderDetails);
router.patch('/:orderId/status', authenticateToken, orderController.updateStatus);
router.post('/:orderId/accept', authenticateToken, orderController.acceptOrder);
router.post('/:orderId/return', authenticateToken, orderController.initiateReturn);
router.get('/:orderId/messages', authenticateToken, orderController.getOrderMessages);
router.post('/upload', authenticateToken, upload.single('file'), (req, res) => {
  if (!req.file) return res.status(400).json({ success: false, message: 'No file uploaded' });
  res.status(200).json({ success: true, url: `/uploads/${req.file.filename}` });
});

module.exports = router;
