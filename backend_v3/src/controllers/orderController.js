const db = require('../config/db');
const geminiService = require('../services/geminiService');

/**
 * Generates a dynamic, distance-based quote.
 */
const getQuote = async (req, res) => {
  const { pickup_address, delivery_address, item_description, pickup_lat, pickup_lng, delivery_lat, delivery_lng } = req.body;
  const userId = req.user?.id;

  // 1. Calculate Distance using PostGIS Geography (Superior precision for V3)
  let distanceKm = 0;
  try {
    const distRes = await db.query(
      "SELECT ST_Distance(ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography, ST_SetSRID(ST_MakePoint($3, $4), 4326)::geography) / 1000 as dist",
      [pickup_lng, pickup_lat, delivery_lng, delivery_lat]
    );
    distanceKm = parseFloat(distRes.rows[0].dist || 0);
  } catch (e) {
    console.error('[Quote] Distance error:', e.message);
  }

  // 2. Classify Size via Gemini v3
  const aiResult = await geminiService.classifyItemSize(item_description);

  // 3. Apply Pricing Formula (Master Brief v3)
  const baseFees = { 'SMALL': 500, 'MEDIUM': 1000, 'LARGE': 1500 };
  const perKmRate = 150;

  const base_fare = baseFees[aiResult.size_tier] || 1000;
  const distance_fare = Math.ceil(distanceKm * perKmRate);
  const total_fare = base_fare + distance_fare;

  // 4. Save Quote
  const quoteRes = await db.query(
    `INSERT INTO quotes (user_id, pickup_address, delivery_address, pickup_location, delivery_location, item_description, size_tier, total_fare)
     VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), ST_SetSRID(ST_MakePoint($6, $7), 4326), $8, $9, $10)
     RETURNING id, expires_at`,
    [userId, pickup_address, delivery_address, pickup_lng, pickup_lat, delivery_lng, delivery_lat, item_description, aiResult.size_tier, total_fare]
  );

  res.status(200).json({
    success: true,
    data: {
      quote_id: quoteRes.rows[0].id,
      size_tier: aiResult.size_tier,
      distance_km: distanceKm.toFixed(2),
      base_fare,
      distance_fare,
      total_fare,
      expires_at: quoteRes.rows[0].expires_at
    }
  });
};

/**
 * Atomically accepts an order.
 */
const acceptOrder = async (req, res) => {
  const { orderId } = req.params;
  const userId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Fetch fulfiller ID for this user
    const fRes = await client.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    if (fRes.rows.length === 0) return res.status(403).json({ success: false, message: 'Fulfiller profile not found' });
    const fulfillerId = fRes.rows[0].id;

    // 2. Atomic claim using SELECT FOR UPDATE
    const { rows } = await client.query(
        "SELECT id, status FROM orders WHERE id = $1 FOR UPDATE",
        [orderId]
    );

    if (rows.length === 0) {
        await client.query('ROLLBACK');
        return res.status(404).json({ success: false, message: 'Order not found' });
    }

    if (rows[0].status !== 'SEARCHING') {
        await client.query('ROLLBACK');
        return res.status(400).json({ success: false, message: 'Order is no longer available' });
    }

    // 3. Assign Fulfiller
    await client.query(
        "UPDATE orders SET fulfiller_id = $1, status = 'MATCHED', matched_at = CURRENT_TIMESTAMP WHERE id = $2",
        [fulfillerId, orderId]
    );

    await client.query('COMMIT');
    console.log(`[Dispatch] Order ${orderId} CLAIMED by Fulfiller ${fulfillerId}`);

    // Notify participants via socket
    const socketService = require('../services/socketService');
    socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'MATCHED' });

    res.status(200).json({
      success: true,
      message: 'Mission Accepted',
      data: { status: 'MATCHED' }
    });

  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
};

/**
 * Returns mission details with coordinates.
 */
const getOrderDetails = async (req, res) => {
  const { orderId } = req.params;
  try {
    const { rows } = await db.query(
      `SELECT o.*,
       ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
       ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng,
       f.full_name as fulfiller_name, f.primary_class, f.mobility_type,
       ST_Y(f.current_location::geometry) as fulfiller_lat, ST_X(f.current_location::geometry) as fulfiller_lng
       FROM orders o
       LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
       WHERE o.id = $1`,
      [orderId]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Mission not found' });
    res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

/**
 * Updates order status and emits socket event.
 */
const updateStatus = async (req, res) => {
  const { orderId } = req.params;
  const { status } = req.body;
  const userId = req.user.id;

  try {
    const { rows } = await db.query(
        "UPDATE orders SET status = $1 WHERE id = $2 RETURNING id, status",
        [status, orderId]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Mission not found' });

    // Sync participants
    const socketService = require('../services/socketService');
    socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status });

    res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

module.exports = {
  getQuote,
  acceptOrder,
  getOrderDetails,
  updateStatus
};
