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
router.get('/guest/:orderId', orderController.getGuestTracking); // PUBLIC
router.get('/guest/checkout/:orderId', orderController.getGuestCheckout); // PUBLIC
router.get('/consent/:orderId', orderController.getConsentPage); // PUBLIC
router.post('/consent/:orderId/grant', orderController.grantConsent); // PUBLIC
router.get('/', authenticateToken, orderController.getUserOrders);
router.post('/', authenticateToken, orderController.createOrder);
router.get('/:orderId', authenticateToken, orderController.getOrderDetails);
router.post('/:orderId/acknowledge', authenticateToken, orderController.acknowledgeOrder);
router.post('/:orderId/timeout-choice', authenticateToken, orderController.handleAcknowledgmentTimeoutChoice);
router.post('/:orderId/cancel', authenticateToken, orderController.cancelOrder);
router.patch('/:orderId/status', authenticateToken, orderController.updateStatus);
router.post('/:orderId/sos', authenticateToken, orderController.triggerSOS);
router.post('/:orderId/accept', authenticateToken, orderController.acceptOrder);
router.post('/:orderId/pickup', authenticateToken, orderController.verifyPickup);
router.post('/:orderId/deliver', authenticateToken, orderController.verifyDelivery);
router.post('/:orderId/request-consent', authenticateToken, orderController.requestConsent);
router.post('/:orderId/fail', authenticateToken, orderController.failDelivery);
router.post('/:orderId/confirm-receipt', authenticateToken, orderController.confirmReceipt);
router.post('/:orderId/dispute', authenticateToken, orderController.reportProblem);
router.post('/:orderId/rate-fulfiller', authenticateToken, orderController.rateFulfiller);
// v4.5 fix: the Android app's agent-side RatingDialog (ActiveOrderScreen.kt)
// posts to POST /api/v1/orders/:id/rate to let the agent rate the customer
// after completing a delivery, and orderController.rateCustomer already
// implements this -- but no route ever pointed to it, so every submission
// 404'd ("Rating failed: ..." toast). Wiring it up here.
router.post('/:orderId/rate', authenticateToken, orderController.rateCustomer);
router.post('/:orderId/incident', authenticateToken, orderController.fileIncident);
router.post('/:orderId/return', authenticateToken, orderController.initiateReturn);
router.patch('/:orderId/reschedule', authenticateToken, orderController.rescheduleOrder);
router.get('/:orderId/messages', authenticateToken, orderController.getOrderMessages);
router.post('/upload', authenticateToken, upload.single('file'), (req, res) => {
  if (!req.file) return res.status(400).json({ success: false, message: 'No file uploaded' });
  res.status(200).json({ success: true, url: `/uploads/${req.file.filename}` });
});

module.exports = router;
