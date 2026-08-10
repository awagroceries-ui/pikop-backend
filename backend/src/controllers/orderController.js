const db = require('../config/db');
const walletService = require('../services/walletService');
const geminiService = require('../services/geminiService');
const socketService = require('../services/socketService');
const crypto = require('crypto');
const bcrypt = require('bcrypt');

/**
 * Helper to fetch public profile data for a fulfiller.
 * Omit sensitive fields like phone or exact location.
 */
const getFulfillerPublicProfile = async (fulfillerId) => {
  if (!fulfillerId) return null;
  const { rows } = await db.query(`
    SELECT u.full_name, f.profile_photo_url, f.tier, f.primary_class, v.registration_number
    FROM fulfillers f
    JOIN users u ON u.id = f.user_id
    LEFT JOIN vehicles v ON v.fulfiller_id = f.id
    WHERE f.id = $1`, [fulfillerId]);

  if (rows.length === 0) return null;
  const p = rows[0];

  return {
    full_name: p.full_name,
    profile_photo_url: p.profile_photo_url,
    tier: p.tier,
    // Only include plate for riders/drivers
    vehicle_registration_number: ['rider', 'driver'].includes(p.primary_class) ? p.registration_number : undefined,
    rating_avg: 4.8 // Placeholder until rating system is built
  };
};

/**
 * Helper to log status history and emit socket events.
 */
const logStatusChange = async (client, orderId, status, description) => {
  await client.query(
    "INSERT INTO order_status_history (order_id, status, description) VALUES ($1, $2, $3)",
    [orderId, status, description]
  );

  if (status === 'ARRIVED_AT_DELIVERY') {
    const deliveryCode = crypto.randomInt(1000, 9999).toString();
    const hash = await bcrypt.hash(deliveryCode, 10);
    await client.query("UPDATE orders SET delivery_code_hash = $1 WHERE id = $2", [hash, orderId]);
    console.log(`[POD] Delivery Code for Order ${orderId}: ${deliveryCode}`);

    try {
        const io = socketService.getIO();
        io.to(`order_${orderId}`).emit("delivery_code_ready", { message: "Fulfiller has arrived." });
    } catch (e) {}
  }

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
    res.status(500).json({ error: 'Failed to generate quote' });
  }
};

/**
 * Creates an order from a valid quote.
 */
const createOrder = async (req, res) => {
  const { quote_id, pickup_lat, pickup_lng, delivery_lat, delivery_lng, item_photo_url, pickup_display_summary, delivery_display_summary } = req.body;
  const userId = req.user?.id || 1;

  if (!item_photo_url) return res.status(400).json({ error: 'Item photo is required' });

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1 AND expires_at > CURRENT_TIMESTAMP", [quote_id]);
    if (quoteRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Quote expired or not found' });
    }

    const quote = quoteRes.rows[0];
    const pickupCode = crypto.randomInt(1000, 9999).toString();
    const pickupHash = await bcrypt.hash(pickupCode, 10);

    const eligibilityMap = { 'SMALL': ['agent', 'rider'], 'MEDIUM': ['rider', 'driver'], 'LARGE': ['driver'] };
    const eligibleClasses = eligibilityMap[quote.size_tier] || ['driver'];

    const { rows } = await client.query(
      `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code_hash, item_photo_url, pickup_display_summary, delivery_display_summary, eligible_classes)
       VALUES ($1, ST_SetSRID(ST_MakePoint($2, $3), 4326), ST_SetSRID(ST_MakePoint($4, $5), 4326), $6, $7, 'SEARCHING', $8, $9, $10, $11, $12, $13)
       RETURNING id, status`,
      [userId, pickup_lng || 0, pickup_lat || 0, delivery_lng || 0, delivery_lat || 0, quote.pickup_address, quote.delivery_address, quote.total_fare, pickupHash, item_photo_url, pickup_display_summary, delivery_display_summary, eligibleClasses]
    );

    const orderId = rows[0].id;
    await logStatusChange(client, orderId, 'SEARCHING', 'Order placed');

    await client.query('COMMIT');
    res.status(201).json({
      order_id: orderId,
      status: rows[0].status,
      pickup_code: pickupCode,
      message: 'Order created'
    });
  } catch (error) {
    await client.query('ROLLBACK');
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

    const { rows } = await client.query("SELECT id, status FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    if (rows[0].status !== 'SEARCHING') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Order already taken' });
    }

    await client.query("UPDATE orders SET fulfiller_id = $1, status = 'MATCHED' WHERE id = $2", [fulfillerId, orderId]);
    await logStatusChange(client, orderId, 'MATCHED', 'Driver assigned');

    const profile = await getFulfillerPublicProfile(fulfillerId);

    await client.query('COMMIT');
    res.status(200).json({
      message: 'Order accepted',
      status: 'MATCHED',
      fulfiller_profile: profile
    });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Failed to accept order' });
  } finally {
    client.release();
  }
};

/**
 * Verifies pickup using a secure hash check.
 */
const verifyPickup = async (req, res) => {
  const { orderId } = req.params;
  const { code } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query("SELECT pickup_code_hash, status FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    const isMatch = await bcrypt.compare(code, rows[0].pickup_code_hash);
    if (!isMatch) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Invalid pickup code' });
    }

    await client.query("UPDATE orders SET status = 'PICKED_UP' WHERE id = $1", [orderId]);
    await logStatusChange(client, orderId, 'PICKED_UP', 'In transit');

    await client.query('COMMIT');
    res.status(200).json({ message: 'Pickup verified', status: 'PICKED_UP' });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: error.message });
  } finally {
    client.release();
  }
};

/**
 * Verifies delivery with GPS metadata capture.
 */
const verifyDelivery = async (req, res) => {
  const { orderId } = req.params;
  const { code, delivery_photo_url, lat, lng } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query("SELECT delivery_code_hash, status FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    const isMatch = await bcrypt.compare(code, rows[0].delivery_code_hash);
    if (!isMatch) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Invalid delivery code' });
    }

    await client.query(
      `UPDATE orders SET status = 'DELIVERED', delivery_photo_url = $1, capture_lat = $2, capture_lng = $3, capture_timestamp = CURRENT_TIMESTAMP WHERE id = $4`,
      [delivery_photo_url, lat || null, lng || null, orderId]
    );

    await logStatusChange(client, orderId, 'DELIVERED', 'Delivered');
    await walletService.processDeliveryPayment(orderId);

    await client.query('COMMIT');
    res.status(200).json({ message: 'Delivery completed', status: 'DELIVERED' });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Failed to verify delivery' });
  } finally {
    client.release();
  }
};

/**
 * Returns full order details including fulfiller public profile.
 */
const getOrderDetails = async (req, res) => {
  const { orderId } = req.params;
  try {
    const orderRes = await db.query(
      `SELECT *,
              ST_Y(pickup_location::geometry) as pickup_lat, ST_X(pickup_location::geometry) as pickup_lng,
              ST_Y(delivery_location::geometry) as delivery_lat, ST_X(delivery_location::geometry) as delivery_lng
       FROM orders WHERE id = $1`, [orderId]);

    if (orderRes.rows.length === 0) return res.status(404).json({ error: 'Order not found' });

    const order = orderRes.rows[0];
    const profile = await getFulfillerPublicProfile(order.fulfiller_id);
    const history = await db.query("SELECT status, description, created_at as time FROM order_status_history WHERE order_id = $1 ORDER BY created_at ASC", [orderId]);

    res.status(200).json({
      ...order,
      fulfiller_profile: profile,
      history: history.rows
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
      `SELECT id, status, total_fare, pickup_address, delivery_address, created_at
       FROM orders WHERE user_id = $1 ORDER BY created_at DESC`, [userId]);
    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch orders' });
  }
};

/**
 * Updates an order status (e.g., ARRIVED_AT_DELIVERY).
 */
const updateOrderStatus = async (req, res) => {
  const { orderId } = req.params;
  const { status } = req.body;

  try {
    const { rows } = await db.query("UPDATE orders SET status = $1 WHERE id = $2 RETURNING id, status", [status, orderId]);
    if (rows.length === 0) return res.status(404).json({ error: 'Order not found' });

    await logStatusChange(db, orderId, status, `Mission updated to ${status}`);
    res.status(200).json(rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

module.exports = {
  getQuote, createOrder, acceptOrder, updateOrderStatus,
  verifyPickup, verifyDelivery, getOrderDetails, getUserOrders,
  cancelOrder: async () => {}, // TODO
  fileIncident: async () => {}, // TODO
  getQueueCandidates: async () => {}, // TODO
  claimQueueOrder: async () => {} // TODO
};
