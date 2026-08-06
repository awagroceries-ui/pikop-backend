const db = require('../config/db');
const walletService = require('../services/walletService');
const geminiService = require('../services/geminiService');

/**
 * Generates a fare quote using Gemini for item size classification.
 */
const getQuote = async (req, res) => {
  const { pickup_address, delivery_address, item_description } = req.body;
  const userId = req.user?.id || 1;

  try {
    const classification = await geminiService.classifyItemSize(item_description);
    const pricing = { 'SMALL': 500.00, 'MEDIUM': 1000.00, 'LARGE': 2000.00 };
    const fare = pricing[classification.size_tier] || 2000.00;

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
    const quoteRes = await db.query(
      "SELECT * FROM quotes WHERE id = $1 AND expires_at > CURRENT_TIMESTAMP",
      [quote_id]
    );

    if (quoteRes.rows.length === 0) {
      return res.status(400).json({ error: 'Quote expired or not found' });
    }

    const quote = quoteRes.rows[0];
    const pickupCode = Math.floor(1000 + Math.random() * 9000).toString();
    const deliveryCode = Math.floor(1000 + Math.random() * 9000).toString();

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
  const { orderId } = req.params;
  const { fulfillerId } = req.body;

  const client = await db.pool.connect();

  try {
    await client.query('BEGIN');

    const orderQuery = `
      SELECT id, status
      FROM orders
      WHERE id = $1
      FOR UPDATE
    `;
    const { rows } = await client.query(orderQuery, [orderId]);

    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    const order = rows[0];

    if (order.status !== 'SEARCHING') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Order already accepted or cancelled' });
    }

    const updateQuery = `
      UPDATE orders
      SET fulfiller_id = $1, status = 'MATCHED'
      WHERE id = $2
    `;
    await client.query(updateQuery, [fulfillerId, orderId]);

    await client.query('COMMIT');

    res.status(200).json({
      message: 'Order accepted successfully',
      orderId,
      status: 'MATCHED'
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error accepting order:', error);
    res.status(500).json({ error: 'Failed to accept order' });
  } finally {
    client.release();
  }
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

    await db.query(
      "UPDATE orders SET status = 'DELIVERED' WHERE id = $1",
      [orderId]
    );

    await walletService.processDeliveryPayment(orderId);

    res.status(200).json({ message: 'Delivery completed and payment processed', status: 'DELIVERED' });
  } catch (error) {
    console.error('Verify Delivery Error:', error);
    res.status(500).json({ error: 'Failed to verify delivery' });
  }
};

module.exports = {
  getQuote,
  createOrder,
  acceptOrder,
  verifyPickup,
  verifyDelivery
};
