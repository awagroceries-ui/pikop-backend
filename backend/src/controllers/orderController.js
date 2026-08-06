const db = require('../config/db');
const walletService = require('../services/walletService');
const geminiService = require('../services/geminiService');

/**
 * Generates a fare quote using Gemini for item size classification.
 */
const getQuote = async (req, res) => {
  const { pickup_address, delivery_address, item_description } = req.body;
  const userId = req.user?.id || 1; // In prod, get from JWT

  try {
    // 1. Classify size with Gemini
    const classification = await geminiService.classifyItemSize(item_description);

    // 2. Map size to price
    const pricing = { 'SMALL': 500.00, 'MEDIUM': 1000.00, 'LARGE': 2000.00 };
    const fare = pricing[classification.size_tier] || 2000.00;

    // 3. Save quote
    const { rows } = await db.query(
      `INSERT INTO quotes (user_id, pickup_address, delivery_address, item_description, size_tier, total_fare, confidence_score)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING id, size_tier, total_fare, expires_at`,
      [userId, pickup_address, delivery_address, item_description, classification.size_tier, fare, classification.confidence]
    );

    const quote = rows[0];

    res.status(200).json({
      quote_id: quote.id,
      fare_breakdown: {
        total_fare: parseFloat(quote.total_fare),
        size_tier: quote.size_tier,
        fare_locked_until: quote.expires_at
      }
    });
  } catch (error) {
    console.error('Get Quote Error:', error);
    res.status(500).json({ error: 'Failed to generate quote' });
  }
};

/**
 * Creates an order from a valid quote.
 */
const createOrder = async (req, res) => {
  const { quote_id, payment_method, recipient_name, recipient_phone, notes } = req.body;
  const userId = req.user?.id || 1;

  try {
    // 1. Fetch and validate quote
    const quoteRes = await db.query(
      "SELECT * FROM quotes WHERE id = $1 AND expires_at > CURRENT_TIMESTAMP",
      [quote_id]
    );

    if (quoteRes.rows.length === 0) {
      return res.status(400).json({ error: 'Quote expired or not found' });
    }

    const quote = quoteRes.rows[0];

    // 2. Generate random verification codes
    const pickupCode = Math.floor(1000 + Math.random() * 9000).toString();
    const deliveryCode = Math.floor(1000 + Math.random() * 9000).toString();

    // 3. Create order (using mock points for geography columns in alpha)
    const { rows } = await db.query(
      `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code, delivery_code)
       VALUES ($1, ST_GeographyFromText('POINT(0 0)'), ST_GeographyFromText('POINT(0 0)'), $2, $3, 'SEARCHING', $4, $5, $6)
       RETURNING id, status`,
      [userId, quote.pickup_address, quote.delivery_address, quote.total_fare, pickupCode, deliveryCode]
    );

    res.status(201).json({
      order_id: rows[0].id,
      status: rows[0].status,
      message: 'Order created and searching for fulfillers'
    });
  } catch (error) {
    console.error('Create Order Error:', error);
    res.status(500).json({ error: 'Failed to create order' });
  }
};

/**
 * Atomically accepts an order by a fulfiller.
 */
const acceptOrder = async (req, res) => {
  // ... (existing logic)
};

/**
 * Verifies pickup using a 4/6-digit code.
 */
const verifyPickup = async (req, res) => {
  const { orderId } = req.params;
  const { code } = req.body;

  try {
    const { rows } = await db.query(
      "SELECT id, pickup_code, status FROM orders WHERE id = $1",
      [orderId]
    );

    if (rows.length === 0) return res.status(404).json({ error: 'Order not found' });
    const order = rows[0];

    if (order.status !== 'MATCHED') {
      return res.status(400).json({ error: 'Order is not in a state for pickup' });
    }

    if (order.pickup_code !== code) {
      return res.status(400).json({ error: 'Invalid pickup code' });
    }

    await db.query(
      "UPDATE orders SET status = 'PICKED_UP' WHERE id = $1",
      [orderId]
    );

    res.status(200).json({ message: 'Pickup verified', status: 'PICKED_UP' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to verify pickup' });
  }
};

/**
 * Verifies delivery and triggers payment split.
 */
const verifyDelivery = async (req, res) => {
  const { orderId } = req.params;
  const { code } = req.body;

  try {
    const { rows } = await db.query(
      "SELECT id, delivery_code, status FROM orders WHERE id = $1",
      [orderId]
    );

    if (rows.length === 0) return res.status(404).json({ error: 'Order not found' });
    const order = rows[0];

    if (order.status !== 'PICKED_UP') {
      return res.status(400).json({ error: 'Order has not been picked up yet' });
    }

    if (order.delivery_code !== code) {
      return res.status(400).json({ error: 'Invalid delivery code' });
    }

    // Mark as delivered
    await db.query(
      "UPDATE orders SET status = 'DELIVERED' WHERE id = $1",
      [orderId]
    );

    // Trigger Wallet Payment Split (75/25)
    await walletService.processDeliveryPayment(orderId);

    res.status(200).json({ message: 'Delivery completed and payment processed', status: 'DELIVERED' });
  } catch (error) {
    console.error('Verify Delivery Error:', error);
    res.status(500).json({ error: 'Failed to verify delivery' });
  }
};

module.exports = {
  acceptOrder,
  verifyPickup,
  verifyDelivery
};
