const db = require('../config/db');
const walletService = require('../services/walletService');
const geminiService = require('../services/geminiService');
const socketService = require('../services/socketService');

/**
 * Helper to log status history and emit socket events.
 */
const logStatusChange = async (client, orderId, status, description) => {
  await client.query(
    "INSERT INTO order_status_history (order_id, status, description) VALUES ($1, $2, $3)",
    [orderId, status, description]
  );
  try {
    const io = socketService.getIO();
    io.to(`order_${orderId}`).emit("status_updated", { status, description, timestamp: new Date().toISOString() });
  } catch (e) {
    console.error("Socket emission failed:", e.message);
  }
};

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

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const quoteRes = await client.query(
      "SELECT * FROM quotes WHERE id = $1 AND expires_at > CURRENT_TIMESTAMP",
      [quote_id]
    );

    if (quoteRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Quote expired or not found' });
    }

    const quote = quoteRes.rows[0];
    const pickupCode = Math.floor(1000 + Math.random() * 9000).toString();
    const deliveryCode = Math.floor(1000 + Math.random() * 9000).toString();

    const { rows } = await client.query(
      `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code, delivery_code)
       VALUES ($1, ST_GeographyFromText('POINT(0 0)'), ST_GeographyFromText('POINT(0 0)'), $2, $3, 'SEARCHING', $4, $5, $6)
       RETURNING id, status`,
      [userId, quote.pickup_address, quote.delivery_address, quote.total_fare, pickupCode, deliveryCode]
    );

    const orderId = rows[0].id;
    await logStatusChange(client, orderId, 'SEARCHING', 'Order placed and searching for nearby fulfillers');

    await client.query('COMMIT');
    res.status(201).json({
      order_id: orderId,
      status: rows[0].status,
      message: 'Order created and searching for fulfillers'
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Create Order Error:', error);
    res.status(500).json({ error: 'Failed to create order' });
  } finally {
    client.release();
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

    await logStatusChange(client, orderId, 'MATCHED', 'Driver assigned and heading to pickup');

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

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query(
      "SELECT id, pickup_code, status FROM orders WHERE id = $1 FOR UPDATE",
      [orderId]
    );

    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }
    const order = rows[0];

    if (order.status !== 'MATCHED') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Order is not in a state for pickup' });
    }

    if (order.pickup_code !== code) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Invalid pickup code' });
    }

    await client.query(
      "UPDATE orders SET status = 'PICKED_UP' WHERE id = $1",
      [orderId]
    );

    await logStatusChange(client, orderId, 'PICKED_UP', 'Item picked up and in transit to destination');

    await client.query('COMMIT');
    res.status(200).json({ message: 'Pickup verified', status: 'PICKED_UP' });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Failed to verify pickup' });
  } finally {
    client.release();
  }
};

/**
 * Verifies delivery and triggers payment split.
 */
const verifyDelivery = async (req, res) => {
  const { orderId } = req.params;
  const { code } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query(
      "SELECT id, delivery_code, status FROM orders WHERE id = $1 FOR UPDATE",
      [orderId]
    );

    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }
    const order = rows[0];

    if (order.status !== 'PICKED_UP') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Order has not been picked up yet' });
    }

    if (order.delivery_code !== code) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Invalid delivery code' });
    }

    await client.query(
      "UPDATE orders SET status = 'DELIVERED' WHERE id = $1",
      [orderId]
    );

    await logStatusChange(client, orderId, 'DELIVERED', 'Item delivered successfully');

    await walletService.processDeliveryPayment(orderId);

    await client.query('COMMIT');
    res.status(200).json({ message: 'Delivery completed and payment processed', status: 'DELIVERED' });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Verify Delivery Error:', error);
    res.status(500).json({ error: 'Failed to verify delivery' });
  } finally {
    client.release();
  }
};

/**
 * Returns full order details and status history.
 */
const getOrderDetails = async (req, res) => {
  const { orderId } = req.params;
  try {
    const orderRes = await db.query("SELECT * FROM orders WHERE id = $1", [orderId]);
    if (orderRes.rows.length === 0) return res.status(404).json({ error: 'Order not found' });

    const historyRes = await db.query(
      "SELECT status, description, created_at as time FROM order_status_history WHERE order_id = $1 ORDER BY created_at ASC",
      [orderId]
    );

    res.status(200).json({
      ...orderRes.rows[0],
      history: historyRes.rows
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch order details' });
  }
};

/**
 * Returns all orders for the authenticated user.
 */
const getUserOrders = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
      "SELECT id, status, total_fare, pickup_address, delivery_address, created_at FROM orders WHERE user_id = $1 ORDER BY created_at DESC",
      [userId]
    );
    res.status(200).json(rows);
  } catch (error) {
    console.error('Get User Orders Error:', error);
    res.status(500).json({ error: 'Failed to fetch your orders' });
  }
};

module.exports = {
  getQuote,
  createOrder,
  acceptOrder,
  verifyPickup,
  verifyDelivery,
  getOrderDetails,
  getUserOrders
};
