const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
const { authenticateToken, requireEmailVerified } = require('../middleware/authMiddleware');

const multer = require('multer');
const path = require('path');

// Setup Multer for disk storage
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

// Quoting (Available to all authenticated users)
router.post('/quote', authenticateToken, orderController.getQuote);
router.post('/upload', authenticateToken, upload.single('document'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file uploaded' });
  res.status(200).json({ url: `/uploads/${req.file.filename}` });
});

// Order Lifecycle (Requires verified email)
router.get('/', authenticateToken, orderController.getUserOrders);
router.get('/:orderId', authenticateToken, orderController.getOrderDetails);
router.post('/', authenticateToken, requireEmailVerified, orderController.createOrder);
router.post('/:orderId/accept', authenticateToken, orderController.acceptOrder);
router.patch('/:orderId/status', authenticateToken, orderController.updateOrderStatus);
router.post('/:orderId/pickup', authenticateToken, orderController.verifyPickup);
router.post('/:orderId/deliver', authenticateToken, orderController.verifyDelivery);
router.post('/:orderId/cancel', authenticateToken, orderController.cancelOrder);
router.post('/:orderId/incident', authenticateToken, orderController.fileIncident);
router.post('/:orderId/rate', authenticateToken, orderController.rateCustomer);

// Chat (Prompt 1)
router.get('/:orderId/messages', authenticateToken, orderController.getMessages);

router.get('/me/queue-candidates', authenticateToken, orderController.getQueueCandidates);
router.post('/:orderId/queue/claim', authenticateToken, orderController.claimQueueOrder);

module.exports = router;
