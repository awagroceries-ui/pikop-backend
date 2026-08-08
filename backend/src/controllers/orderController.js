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
  const { pickup_address, delivery_address, item_description, pickup_lat, pickup_lng, delivery_lat, delivery_lng } = req.body;
  const userId = req.user?.id || 1;

  try {
    const classification = await geminiService.classifyItemSize(item_description);
    const pricing = { 'SMALL': 500.00, 'MEDIUM': 1000.00, 'LARGE': 2000.00 };
    const fare = pricing[classification.size_tier] || 2000.00;

    const { rows } = await db.query(
      `INSERT INTO quotes (user_id, pickup_address, delivery_address, item_description, size_tier, total_fare, confidence_score, pickup_location, delivery_location)
       VALUES ($1, $2, $3, $4, $5, $6, $7, ST_SetSRID(ST_MakePoint($8, $9), 4326), ST_SetSRID(ST_MakePoint($10, $11), 4326))
       RETURNING id, size_tier, total_fare, expires_at`,
      [userId, pickup_address, delivery_address, item_description, classification.size_tier, fare, classification.confidence, pickup_lng || 0, pickup_lat || 0, delivery_lng || 0, delivery_lat || 0]
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
  const { quote_id, payment_method, recipient_name, recipient_phone, notes, pickup_lat, pickup_lng, delivery_lat, delivery_lng } = req.body;
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

    // Use provided coordinates if available, otherwise default to POINT(0 0)
    const pLat = pickup_lat || 0;
    const pLng = pickup_lng || 0;
    const dLat = delivery_lat || 0;
    const dLng = delivery_lng || 0;

    const { rows } = await client.query(
      `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code, delivery_code)
       VALUES ($1, ST_SetSRID(ST_MakePoint($2, $3), 4326), ST_SetSRID(ST_MakePoint($4, $5), 4326), $6, $7, 'SEARCHING', $8, $9, $10)
       RETURNING id, status`,
      [userId, pLng, pLat, dLng, dLat, quote.pickup_address, quote.delivery_address, quote.total_fare, pickupCode, deliveryCode]
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
    const orderRes = await db.query(
      `SELECT *,
              ST_Y(pickup_location::geometry) as pickup_lat,
              ST_X(pickup_location::geometry) as pickup_lng,
              ST_Y(delivery_location::geometry) as delivery_lat,
              ST_X(delivery_location::geometry) as delivery_lng
       FROM orders WHERE id = $1`,
      [orderId]
    );
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
    console.error('Get Order Details Error:', error);
    res.status(500).json({ error: 'Failed to fetch order details' });
  }
};

/**
 * Cancels an order and applies fees if matched.
 */
const cancelOrder = async (req, res) => {
  const userId = req.user.id;
  const { orderId } = req.params;
  const { reason } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const { rows } = await client.query(
      "SELECT id, status, total_fare, user_id FROM orders WHERE id = $1 FOR UPDATE",
      [orderId]
    );

    if (rows.length === 0) return res.status(404).json({ error: 'Order not found' });
    const order = rows[0];

    // Ensure only the owner or an admin can cancel
    if (order.user_id !== userId) return res.status(403).json({ error: 'Unauthorized' });

    if (['DELIVERED', 'CANCELLED'].includes(order.status)) {
      return res.status(400).json({ error: 'Order cannot be cancelled in current state' });
    }

    let fee = 0;
    if (order.status === 'MATCHED') {
      fee = 200.00; // Standard cancellation fee
    } else if (order.status === 'PICKED_UP') {
       return res.status(400).json({ error: 'Item already picked up. Contact support to cancel.' });
    }

    // Update Order Status
    await client.query(
      "UPDATE orders SET status = 'CANCELLED' WHERE id = $1",
      [orderId]
    );

    await logStatusChange(client, orderId, 'CANCELLED', `Order cancelled by user. Reason: ${reason || 'Not provided'}`);

    if (fee > 0) {
      // Deduct fee from user wallet (This logic assumes user has a wallet)
      // For MVP, we'll just log it or flag for next payment if no balance
      await client.query(
        "UPDATE wallets SET balance = balance - $1 WHERE owner_id = $2 AND owner_type = 'USER'",
        [fee, userId]
      );
      await client.query(
        "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ((SELECT id FROM wallets WHERE owner_id = $1 AND owner_type = 'USER'), $2, 'DEBIT', 'CANCELLATION_FEE', $3)",
        [userId, fee, orderId]
      );
    }

    await client.query('COMMIT');
    res.status(200).json({ message: 'Order cancelled successfully', fee_applied: fee });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Cancel Order Error:', error);
    res.status(500).json({ error: 'Failed to cancel order' });
  } finally {
    client.release();
  }
};

module.exports = {
  getQuote,
  createOrder,
  acceptOrder,
  verifyPickup,
  verifyDelivery,
  getOrderDetails,
  getUserOrders,
  cancelOrder
};
