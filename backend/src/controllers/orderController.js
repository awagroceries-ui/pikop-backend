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
  const { quote_id, payment_method, recipient_name, recipient_phone, notes, pickup_lat, pickup_lng, delivery_lat, delivery_lng, item_photo_url, pickup_display_summary, delivery_display_summary } = req.body;
  const userId = req.user?.id || 1;

  if (!item_photo_url) return res.status(400).json({ error: 'Item photo is required' });

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
      `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code, delivery_code, item_photo_url, pickup_display_summary, delivery_display_summary)
       VALUES ($1, ST_SetSRID(ST_MakePoint($2, $3), 4326), ST_SetSRID(ST_MakePoint($4, $5), 4326), $6, $7, 'SEARCHING', $8, $9, $10, $11, $12, $13)
       RETURNING id, status`,
      [userId, pLng, pLat, dLng, dLat, quote.pickup_address, quote.delivery_address, quote.total_fare, pickupCode, deliveryCode, item_photo_url, pickup_display_summary, delivery_display_summary]
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
  const { code, delivery_photo_url } = req.body;

  if (!delivery_photo_url) return res.status(400).json({ error: 'Delivery proof photo is required' });

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
      "UPDATE orders SET status = 'DELIVERED', delivery_photo_url = $1 WHERE id = $2",
      [delivery_photo_url, orderId]
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
      "SELECT id, status, total_fare, user_id, fulfiller_id FROM orders WHERE id = $1 FOR UPDATE",
      [orderId]
    );

    if (rows.length === 0) return res.status(404).json({ error: 'Order not found' });
    const order = rows[0];

    // 1. Logic for Fulfiller Cancellation
    const isFulfiller = req.user.role === 'FULFILLER';
    if (isFulfiller) {
      return res.status(400).json({ error: 'Fulfillers must file an Incident Report to cancel an active mission.' });
    }

    // 2. Logic for User Cancellation
    if (order.user_id !== userId) return res.status(403).json({ error: 'Unauthorized' });

    if (['DELIVERED', 'CANCELLED'].includes(order.status)) {
      return res.status(400).json({ error: 'Order cannot be cancelled in current state' });
    }

    let fee = 0;
    if (['MATCHED', 'PICKED_UP'].includes(order.status)) {
       // After MATCHED, user is charged 25% fee
       fee = (order.total_fare * 0.25).toFixed(2);
    }

    if (order.status === 'PICKED_UP') {
       // Item already with driver - block simple cancellation
       await client.query('ROLLBACK');
       return res.status(400).json({ error: 'Item already picked up. Contact support to cancel.' });
    }

    // Update Order Status
    await client.query(
      "UPDATE orders SET status = 'CANCELLED' WHERE id = $1",
      [orderId]
    );

    await logStatusChange(client, orderId, 'CANCELLED', `Mission aborted by User. Reason: ${reason || 'Not provided'}`);

    if (fee > 0) {
      // 100% of fee goes to platform wallet
      await walletService.processCancellationFee(orderId);

      // Refund remaining 75% to user (This logic would be more complex with real payments)
      // For now, we log the debit of the full amount and credit of 75% if using a wallet
      await client.query(
        "UPDATE wallets SET balance = balance + $1 WHERE owner_id = $2 AND owner_type = 'USER'",
        [(order.total_fare - fee).toFixed(2), userId]
      );
    }

    await client.query('COMMIT');
    res.status(200).json({ message: 'Mission aborted. 25% cancellation protocol applied.', fee_applied: fee });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Cancel Order Error:', error);
    res.status(500).json({ error: 'Failed to abort mission' });
  } finally {
    client.release();
  }
};

/**
 * Files an incident report for an active order (Fulfiller Only).
 */
const fileIncident = async (req, res) => {
  const userId = req.user.id;
  const { orderId } = req.params;
  const { category, description, resolution_requested } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Verify order and fulfiller ownership
    const orderRes = await client.query(
      `SELECT o.*, f.id as fulfiller_real_id
       FROM orders o
       JOIN fulfillers f ON f.id = o.fulfiller_id
       WHERE o.id = $1 AND f.user_id = $2 FOR UPDATE`,
      [orderId, userId]
    );

    if (orderRes.rows.length === 0) return res.status(404).json({ error: 'Active mission not found' });
    const order = orderRes.rows[0];

    // 2. Create Dispute row
    const cat = `incident_${category}`;
    const disputeRes = await client.query(
      `INSERT INTO disputes (order_id, reporter_id, reason, category, status)
       VALUES ($1, $2, $3, $4, 'OPEN') RETURNING id`,
      [orderId, order.user_id, description, cat]
    );
    const disputeId = disputeRes.rows[0].id;

    // 3. Resolution Logic
    if (resolution_requested === 'handoff') {
      // Reset to SEARCHING
      await client.query(
        "UPDATE orders SET status = 'SEARCHING', fulfiller_id = NULL, incident_dispute_id = $1 WHERE id = $2",
        [disputeId, orderId]
      );
      await logStatusChange(client, orderId, 'SEARCHING', 'Fulfiller reassigned due to an unexpected operational issue');

    } else if (resolution_requested === 'cancel_with_waiver_request') {
      let waiveFee = false;
      if (category === 'security_risk') waiveFee = true;

      await client.query(
        "UPDATE orders SET status = 'CANCELLED', incident_dispute_id = $1, cancellation_fee_waived = $2 WHERE id = $3",
        [disputeId, waiveFee, orderId]
      );

      await logStatusChange(client, orderId, 'CANCELLED', `Mission aborted due to ${category}. Waiver requested.`);

      // Charge 25% fee immediately (even if waiver requested, unless security_risk)
      if (!waiveFee) {
        await walletService.processCancellationFee(orderId);
      }
    }

    await client.query('COMMIT');
    res.status(200).json({ message: 'Incident report filed and processed.', dispute_id: disputeId });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Incident Report Error:', error);
    res.status(500).json({ error: 'Failed to process incident report' });
  } finally {
    client.release();
  }
};

/**
 * Returns all orders for the authenticated user.
 */
const getUserOrders = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
      `SELECT id, status, total_fare, pickup_address, delivery_address, created_at
       FROM orders WHERE user_id = $1 ORDER BY created_at DESC`,
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
  getUserOrders,
  cancelOrder
};
