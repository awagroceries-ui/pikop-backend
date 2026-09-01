const db = require('../config/db');
const geminiService = require('../services/geminiService');
const walletService = require('../services/walletService');

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

  // 3. Apply Dynamic Pricing Dynamics (v3.5.1 Settings-Linked)
  // Standardized defaults for Nigeria market (Calibrated v3.6)
  let baseFees = { 'SMALL': 400, 'MEDIUM': 800, 'LARGE': 1500 };
  let perKmRate = 110;
  let roadWindingFactor = 1.15; // Realistic buffer for Lagos street navigation

  try {
    const settingsRes = await db.query("SELECT key, value FROM settings WHERE key IN ('base_fare_small', 'base_fare_medium', 'base_fare_large', 'per_km_rate')");
    settingsRes.rows.forEach(r => {
        if (r.key === 'base_fare_small') baseFees['SMALL'] = parseFloat(r.value);
        if (r.key === 'base_fare_medium') baseFees['MEDIUM'] = parseFloat(r.value);
        if (r.key === 'base_fare_large') baseFees['LARGE'] = parseFloat(r.value);
        if (r.key === 'per_km_rate') perKmRate = parseFloat(r.value);
    });
  } catch (e) {
    console.warn('[Quote] Settings fetch failed, using fallback pricing.');
  }

  const base_fare = baseFees[aiResult.size_tier] || baseFees['MEDIUM'];
  const effectiveDistance = distanceKm * roadWindingFactor;
  const distance_fare = Math.ceil(effectiveDistance * perKmRate);
  const total_fare = Math.round(base_fare + distance_fare);

  console.log(`[Quote] User: ${userId} | RawDist: ${distanceKm.toFixed(2)}km | RoadDist: ${effectiveDistance.toFixed(2)}km | Base: ${base_fare} | DistFare: ${distance_fare} | Total: ${total_fare}`);

  // 4. Save Quote
  const quoteRes = await db.query(
    `INSERT INTO quotes (user_id, pickup_address, delivery_address, pickup_location, delivery_location, item_description, size_tier, total_fare)
     VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), ST_SetSRID(ST_MakePoint($6, $7), 4326), $8, $9, $10)
     RETURNING id, expires_at`,
    [userId, pickup_address, delivery_address, pickup_lng, pickup_lat, delivery_lng, delivery_lat, item_description, aiResult.size_tier, total_fare]
  );

  res.status(200).json({
    success: true,
    quote_id: quoteRes.rows[0].id,
    size_tier: aiResult.size_tier,
    distance_km: distanceKm.toFixed(2),
    base_fare,
    distance_fare,
    total_fare,
    expires_at: quoteRes.rows[0].expires_at
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

    // 3. Assign Fulfiller or Add to Queue
    const activeCheck = await client.query(
        "SELECT id FROM orders WHERE fulfiller_id = $1 AND status NOT IN ('DELIVERED', 'CANCELLED')",
        [fulfillerId]
    );

    if (activeCheck.rows.length > 0) {
        // Fulfiller is busy, add to queue
        await client.query(
            "UPDATE orders SET queued_for_fulfiller_id = $1, status = 'QUEUED' WHERE id = $2",
            [fulfillerId, orderId]
        );
        console.log(`[Dispatch] Order ${orderId} QUEUED for Fulfiller ${fulfillerId}`);
    } else {
        // Fulfiller is free, assign as primary
        await client.query(
            "UPDATE orders SET fulfiller_id = $1, status = 'MATCHED', matched_at = CURRENT_TIMESTAMP WHERE id = $2",
            [fulfillerId, orderId]
        );
        console.log(`[Dispatch] Order ${orderId} CLAIMED by Fulfiller ${fulfillerId}`);
    }

    await client.query('COMMIT');

    // Notify participants via socket
    const socketService = require('../services/socketService');
    socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: activeCheck.rows.length > 0 ? 'QUEUED' : 'MATCHED' });

    res.status(200).json({
      success: true,
      message: activeCheck.rows.length > 0 ? 'Added to Queue' : 'Mission Accepted',
      data: { status: activeCheck.rows.length > 0 ? 'QUEUED' : 'MATCHED' }
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
    res.status(200).json(rows[0]);
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
    // 1. CoD Gate for Delivery Completion (Milestone 2 Expansion)
    if (status === 'DELIVERED') {
        const { rows: o } = await db.query("SELECT collection_status, collect_on_delivery_amount FROM orders WHERE id = $1", [orderId]);
        if (o[0].collect_on_delivery_amount && o[0].collection_status !== 'collected') {
            return res.status(400).json({
                success: false,
                message: 'Collection required. This order has a mandatory CoD amount that must be paid via app before delivery closure.'
            });
        }
    }

    const { rows } = await db.query(
        "UPDATE orders SET status = $1 WHERE id = $2 RETURNING id, status",
        [status, orderId]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Mission not found' });

    // Sync participants
    const socketService = require('../services/socketService');
    socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status });

    // 2. Trigger Settlement on Delivery (v3)
    if (status === 'DELIVERED') {
        await walletService.processMissionSettlement(orderId);
    }

    // 3. No-Refund Policy for Recipient Absence (v3.8.1)
    if (status === 'RECIPIENT_ABSENT') {
        console.log(`[Policy] Mission #${orderId} marked RECIPIENT_ABSENT. No refund eligible.`);
        // Note: No wallet reverse call here.
    }

    res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

/**
 * Cancels an order.
 */
const cancelOrder = async (req, res) => {
    const { orderId } = req.params;
    const { reason } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query(
            "UPDATE orders SET status = 'CANCELLED', cancellation_reason = $1 WHERE id = $2 AND user_id = $3 AND status NOT IN ('PICKED_UP', 'DELIVERED', 'CANCELLED') RETURNING id",
            [reason || 'User cancelled', orderId, userId]
        );

        if (rows.length === 0) {
            console.warn(`[Cancel] Unauthorized or invalid state for Order ${orderId} by User ${userId}`);
            return res.status(400).json({ success: false, message: 'Order cannot be cancelled. It might be already in progress or completed.' });
        }

        // Notify participants
        const socketService = require('../services/socketService');
        socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'CANCELLED' });

        console.log(`[Cancel] Mission ${orderId} successfully aborted by User ${userId}`);
        res.status(200).json({ success: true, message: 'Mission aborted' });
    } catch (error) {
        console.error(`[Cancel] Error for Order ${orderId}:`, error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Initiates a return mission at 50% of the original fare.
 */
const initiateReturn = async (req, res) => {
    const { orderId } = req.params;
    const userId = req.user.id;

    try {
        const { rows } = await db.query("SELECT * FROM orders WHERE id = $1 AND user_id = $2", [orderId, userId]);
        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Original order not found' });

        const orig = rows[0];
        if (orig.status !== 'RECIPIENT_ABSENT') {
            return res.status(400).json({ success: false, message: 'Return can only be initiated if recipient was absent' });
        }

        const returnFare = parseFloat(orig.total_fare) * 0.5;

        // Create new mission with reversed addresses
        const returnRes = await db.query(
            `INSERT INTO orders (
                order_type, user_id, parent_order_id, status,
                pickup_address, delivery_address,
                pickup_location, delivery_location,
                total_fare, item_description, payment_status
            ) VALUES (
                $1, $2, $3, 'SEARCHING',
                $4, $5, $6, $7, $8, $9, 'pending'
            ) RETURNING id`,
            [
                orig.order_type, userId, orig.id,
                orig.delivery_address, orig.pickup_address, // Reversed
                orig.delivery_location, orig.pickup_location, // Reversed
                returnFare, `RETURN: ${orig.item_description}`
            ]
        );

        res.status(201).json({
            success: true,
            message: 'Return mission created. Please complete payment.',
            data: { return_order_id: returnRes.rows[0].id, amount: returnFare }
        });
    } catch (error) {
        throw error;
    }
};

/**
 * Fetches message history for a specific order.
 */
const getOrderMessages = async (req, res) => {
  const { orderId } = req.params;
  try {
    const { rows } = await db.query(
      "SELECT id, sender_id, sender_type, content, content as text, content as body, created_at, is_read FROM messages WHERE order_id = $1 ORDER BY created_at ASC LIMIT 100",
      [orderId]
    );
    res.status(200).json(rows);
  } catch (error) {
    throw error;
  }
};

/**
 * Finds an order by its source quote ID.
 * Used for auto-return after payment webhook.
 */
const getOrderByQuote = async (req, res) => {
    const { quoteId } = req.params;
    try {
        const { rows } = await db.query(
            "SELECT id, status FROM orders WHERE quote_id = $1 LIMIT 1",
            [quoteId]
        );
        if (rows.length === 0) return res.status(404).json({ success: false });
        res.status(200).json({ success: true, order_id: rows[0].id, status: rows[0].status });
    } catch (e) {
        res.status(500).json({ success: false });
    }
};

/**
 * Manually creates an order from a verified payment.
 * Fallback for delayed webhooks.
 */
const createOrder = async (req, res) => {
    const { quote_id, payment_method, recipient_name, recipient_phone, notes, pickup_display_summary, delivery_display_summary, item_photo_url, promo_id, payment_reference } = req.body;
    const userId = req.user.id;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Check if already active
        const existing = await client.query("SELECT id FROM orders WHERE quote_id = $1", [quote_id]);
        if (existing.rows.length > 0) {
            await client.query('ROLLBACK');
            return res.status(200).json({ success: true, order_id: existing.rows[0].id, status: 'SEARCHING' });
        }

        // 2. Fetch Quote
        const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1", [quote_id]);
        if (quoteRes.rows.length === 0) throw new Error('Quote not found');
        const q = quoteRes.rows[0];

        // 3. Handle Promo/Coupon Server-Side Verification
        let finalFare = parseFloat(q.total_fare);
        let couponId = null;

        if (promo_id) {
            const couponRes = await client.query("SELECT * FROM coupons WHERE id = $1 AND is_active = true", [promo_id]);
            if (couponRes.rows.length > 0) {
                const c = couponRes.rows[0];
                couponId = c.id;
                const discount = c.discount_type === 'FIXED' ? parseFloat(c.discount_value) : finalFare * (parseFloat(c.discount_value) / 100);
                finalFare = Math.max(0, finalFare - discount);
                console.log(`[Order] Applied Promo: ${c.code}. Discount: ${discount}. New Fare: ${finalFare}`);
            }
        }

        // 4. Create Order (DEFINITIVE ALIGNMENT WITH WEBHOOK)
        const orderRes = await client.query(
            `INSERT INTO orders (
                order_type, user_id, quote_id, status,
                item_description, size_tier,
                pickup_address, delivery_address,
                pickup_location, delivery_location,
                total_fare, payment_status, payment_method, payment_reference,
                recipient_name, recipient_phone, notes,
                pickup_display_summary, delivery_display_summary, item_photo_url,
                pickup_code_hash, delivery_code_hash, coupon_id
            ) VALUES (
                'pickup_delivery', $1, $2, 'SEARCHING',
                $3, $4,
                $5, $6,
                $7, $8,
                $9, 'PAID', $10, $11,
                $12, $13, $14, $15, $16, $17,
                'v3_pending', 'v3_pending', $18
            ) RETURNING id`,
            [
                userId, q.id, q.item_description, q.size_tier,
                q.pickup_address, q.delivery_address,
                q.pickup_location, q.delivery_location,
                finalFare, payment_method || 'card', payment_reference,
                recipient_name || 'Recipient',
                recipient_phone || '000',
                notes,
                pickup_display_summary || q.pickup_address.substring(0, 50),
                delivery_display_summary || q.delivery_address.substring(0, 50),
                item_photo_url,
                couponId
            ]
        );

        await client.query('COMMIT');
        console.log(`[ManualOrder] Mission activated: ${orderRes.rows[0].id} for User: ${userId} | Fare: ${finalFare}`);
        res.status(201).json({ success: true, order_id: orderRes.rows[0].id, status: 'SEARCHING' });
    } catch (e) {
        await client.query('ROLLBACK');
        console.error('[ManualOrder] Activation failed. Detailed Error:', {
            message: e.message,
            code: e.code,
            detail: e.detail,
            hint: e.hint,
            table: e.table,
            constraint: e.constraint
        });
        res.status(500).json({ success: false, message: e.message });
    } finally {
        client.release();
    }
};

/**
 * Returns missions for the authenticated customer.
 */
const getUserOrders = async (req, res) => {
  const userId = req.user.id;

  try {
    const { rows } = await db.query(
      `SELECT o.*,
       ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
       ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng
       FROM orders o
       WHERE o.user_id = $1
       ORDER BY o.created_at DESC`,
      [userId]
    );

    res.status(200).json(rows);
  } catch (error) {
    throw error;
  }
};

module.exports = {
  getQuote,
  getOrderByQuote,
  createOrder,
  acceptOrder,
  getOrderDetails,
  updateStatus,
  initiateReturn,
  getOrderMessages,
  getUserOrders,
  cancelOrder
};
