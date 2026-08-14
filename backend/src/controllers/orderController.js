const db = require('../config/db');
const walletService = require('../services/walletService');
const geminiService = require('../services/geminiService');
const socketService = require('../services/socketService');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');

/**
 * Helper to fetch public profile data for a fulfiller.
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
    vehicle_registration_number: ['rider', 'driver'].includes(p.primary_class) ? p.registration_number : undefined,
    rating_avg: 4.8
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
    console.log(`[POD] Secure Delivery Code for Order ${orderId}: ${deliveryCode}`);

    try {
        const io = socketService.getIO();
        io.to(`order_${orderId}`).emit("delivery_code_ready", { message: "Fulfiller has arrived. Your code is ready." });

        // PUSH to user (Implementation from Prompt 6)
        const { rows: userRow } = await client.query("SELECT user_id FROM orders WHERE id = $1", [orderId]);
        if (userRow.length > 0) {
            const fcmService = require('../services/fcmService');
            fcmService.sendNotification(userRow[0].user_id, "Fulfiller Arrived", `Your delivery code is: ${deliveryCode}`);
        }
    } catch (e) {}
  }

  try {
    const io = socketService.getIO();
    io.to(`order_${orderId}`).emit("status_updated", { status, description, timestamp: new Date().toISOString() });

    const { rows } = await client.query("SELECT tracking_token FROM orders WHERE id = $1", [orderId]);
    if (rows.length > 0 && rows[0].tracking_token) {
        io.to(`tracking_${rows[0].tracking_token}`).emit("status_updated", { status, description });
    }
  } catch (e) {}
};

/**
 * Generates a fare quote using Gemini.
 */
const getQuote = async (req, res) => {
  const { pickup_address, delivery_address, item_description, pickup_lat, pickup_lng, delivery_lat, delivery_lng } = req.body;
  const userId = req.user?.id || 1;

  try {
    const classification = await geminiService.classifyItemSize(item_description);
    const pricing = { 'SMALL': 500.00, 'MEDIUM': 1000.00, 'LARGE': 2000.00 };
    const fare = pricing[classification.size_tier] || 2000.00;

    const eligibilityMap = { 'SMALL': ['agent', 'rider'], 'MEDIUM': ['rider', 'driver'], 'LARGE': ['driver'] };
    const eligibleClasses = eligibilityMap[classification.size_tier] || ['driver'];

    const { rows } = await db.query(
      `INSERT INTO quotes (user_id, pickup_address, delivery_address, item_description, size_tier, total_fare, confidence_score, pickup_location, delivery_location)
       VALUES ($1, $2, $3, $4, $5, $6, $7, ST_SetSRID(ST_MakePoint($8, $9), 4326), ST_SetSRID(ST_MakePoint($10, $11), 4326))
       RETURNING id, size_tier, total_fare, expires_at`,
      [userId, pickup_address, delivery_address, item_description, classification.size_tier, fare, classification.confidence, pickup_lng || 0, pickup_lat || 0, delivery_lng || 0, delivery_lat || 0]
    );

    res.status(200).json({
      quote_id: rows[0].id,
      fare_breakdown: {
        total_fare: parseFloat(rows[0].total_fare),
        size_tier: rows[0].size_tier,
        required_classes: eligibleClasses,
        fare_locked_until: rows[0].expires_at
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
  const { quote_id, corporate_account_id, promo_id, pickup_lat, pickup_lng, delivery_lat, delivery_lng, item_photo_url, pickup_display_summary, delivery_display_summary } = req.body;
  const userId = req.user?.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1 AND expires_at > CURRENT_TIMESTAMP", [quote_id]);
    if (quoteRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Quote expired or not found' });
    }
    const quote = quoteRes.rows[0];

    let discount = 0;
    if (promo_id) {
        const promoRes = await client.query("SELECT * FROM promo_codes WHERE id = $1 AND valid_from <= NOW() AND valid_to >= NOW() AND used_count < max_uses", [promo_id]);
        if (promoRes.rows.length > 0) {
            const p = promoRes.rows[0];
            discount = p.discount_type === 'flat' ? parseFloat(p.value) : (parseFloat(quote.total_fare) * (parseFloat(p.value)/100));
        }
    }

    const pickupCode = crypto.randomInt(1000, 9999).toString();
    const pickupHash = await bcrypt.hash(pickupCode, 10);
    const trackingToken = crypto.randomUUID();

    const eligibilityMap = {
        'SMALL': ['agent', 'rider'],
        'MEDIUM': ['rider', 'driver'],
        'LARGE': ['driver']
    };
    const eligibleClasses = eligibilityMap[quote.size_tier] || ['driver'];

    const { rows } = await client.query(
      `INSERT INTO orders (user_id, corporate_account_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code_hash, item_photo_url, pickup_display_summary, delivery_display_summary, eligible_classes, tracking_token, promo_code_id, discount_amount)
       VALUES ($1, $2, ST_SetSRID(ST_MakePoint($3, $4), 4326), ST_SetSRID(ST_MakePoint($5, $6), 4326), $7, $8, 'SEARCHING', $9, $10, $11, $12, $13, $14, $15, $16, $17)
       RETURNING id, status, tracking_token`,
      [userId, corporate_account_id || null, pickup_lng || 0, pickup_lat || 0, delivery_lng || 0, delivery_lat || 0, quote.pickup_address, quote.delivery_address, Math.max(0, parseFloat(quote.total_fare) - discount), pickupHash, item_photo_url, pickup_display_summary, delivery_display_summary, eligibleClasses, trackingToken, promo_id || null, discount]
    );

    const orderId = rows[0].id;

    if (corporate_account_id) {
        await walletService.processCorporateDebit(client, corporate_account_id, quote.total_fare, orderId);
    }
    if (promo_id) {
        await client.query("UPDATE promo_codes SET used_count = used_count + 1 WHERE id = $1", [promo_id]);
        await client.query("INSERT INTO promo_code_redemptions (promo_code_id, user_id, order_id) VALUES ($1, $2, $3)", [promo_id, userId, orderId]);
    }

    await logStatusChange(client, orderId, 'SEARCHING', 'Order placed');
    await client.query('COMMIT');

    res.status(201).json({
      order_id: orderId,
      status: rows[0].status,
      pickup_code: pickupCode,
      tracking_url: `https://api.awa.name.ng/track/${rows[0].tracking_token}`,
      message: 'Order created'
    });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: error.message });
  } finally {
    client.release();
  }
};

/**
 * Atomically accepts an order.
 */
const acceptOrder = async (req, res) => {
  const { orderId } = req.params;
  const { fulfillerId } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query("SELECT id, status FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0 || rows[0].status !== 'SEARCHING') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Order no longer available' });
    }

    await client.query("UPDATE orders SET fulfiller_id = $1, status = 'MATCHED' WHERE id = $2", [fulfillerId, orderId]);
    await logStatusChange(client, orderId, 'MATCHED', 'Driver assigned');
    const profile = await getFulfillerPublicProfile(fulfillerId);

    await client.query('COMMIT');
    res.status(200).json({ message: 'Order accepted', status: 'MATCHED', fulfiller_profile: profile });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Accept failed' });
  } finally {
    client.release();
  }
};

/**
 * Verifies pickup.
 */
const verifyPickup = async (req, res) => {
  const { orderId } = req.params;
  const { code } = req.body;
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query("SELECT pickup_code_hash FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0 || !await bcrypt.compare(code, rows[0].pickup_code_hash)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Invalid code' });
    }
    await client.query("UPDATE orders SET status = 'PICKED_UP' WHERE id = $1", [orderId]);
    await logStatusChange(client, orderId, 'PICKED_UP', 'In transit');
    await client.query('COMMIT');
    res.status(200).json({ message: 'Pickup verified', status: 'PICKED_UP' });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Verify failed' });
  } finally {
    client.release();
  }
};

/**
 * Verifies delivery.
 */
const verifyDelivery = async (req, res) => {
  const { orderId } = req.params;
  const { code, delivery_photo_url, lat, lng, device_timestamp } = req.body;
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query("SELECT delivery_code_hash FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0 || !await bcrypt.compare(code, rows[0].delivery_code_hash)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Invalid code' });
    }

    // --- SECURE POD CROSS-CHECK (Prompt 13) ---
    const metadata = { gps_mismatch: false, timestamp_mismatch: false };
    const serverTime = new Date();

    if (device_timestamp) {
        const diff = Math.abs(serverTime.getTime() - device_timestamp);
        if (diff > 10 * 60 * 1000) { // 10 minutes (Prompt 13, point 3)
            metadata.timestamp_mismatch = true;
            console.warn(`[POD] Timestamp Mismatch detected for Order ${orderId}: ${diff / 1000}s`);
        }
    }

    if (lat && lng) {
        // Query to check distance between capture point and actual delivery location
        const distRes = await client.query(`
            SELECT ST_Distance(
                ST_SetSRID(ST_MakePoint($1, $2), 4326),
                delivery_location
            ) as distance FROM orders WHERE id = $3
        `, [lng, lat, orderId]);

        if (distRes.rows.length > 0 && distRes.rows[0].distance > 200) {
            metadata.gps_mismatch = true;
            console.warn(`[POD] GPS Mismatch detected for Order ${orderId}: ${distRes.rows[0].distance}m`);
        }
    }

    await client.query(
        `UPDATE orders SET
            status = 'DELIVERED',
            delivery_photo_url = $1,
            capture_lat = $2,
            capture_lng = $3,
            capture_timestamp = $4,
            verification_metadata = $5
         WHERE id = $6`,
        [delivery_photo_url, lat || null, lng || null, serverTime, JSON.stringify(metadata), orderId]
    );

    await logStatusChange(client, orderId, 'DELIVERED', 'Delivered');
    await walletService.processDeliveryPayment(orderId);
    await walletService.triggerReferralReward(client, orderId);
    await client.query('COMMIT');

    // Prompt 7: Check for queued order
    const nextOrder = await db.query("SELECT id FROM orders WHERE queued_for_fulfiller_id = (SELECT fulfiller_id FROM orders WHERE id = $1) AND status = 'QUEUED' LIMIT 1", [orderId]);
    if (nextOrder.rows.length > 0) {
        const nextId = nextOrder.rows[0].id;
        await db.query("UPDATE orders SET status = 'EN_ROUTE_TO_PICKUP', fulfiller_id = (SELECT fulfiller_id FROM orders WHERE id = $1), queued_for_fulfiller_id = NULL WHERE id = $2", [orderId, nextId]);
        await logStatusChange(db, nextId, 'EN_ROUTE_TO_PICKUP', 'System activated your next queued mission');
    }

    res.status(200).json({ message: 'Delivery completed', status: 'DELIVERED' });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Verify failed' });
  } finally {
    client.release();
  }
};

/**
 * Returns full order details.
 */
const getOrderDetails = async (req, res) => {
  const { orderId } = req.params;
  try {
    const orderRes = await db.query(`SELECT *, ST_Y(pickup_location::geometry) as pickup_lat, ST_X(pickup_location::geometry) as pickup_lng, ST_Y(delivery_location::geometry) as delivery_lat, ST_X(delivery_location::geometry) as delivery_lng FROM orders WHERE id = $1`, [orderId]);
    if (orderRes.rows.length === 0) return res.status(404).json({ error: 'Order not found' });
    const order = orderRes.rows[0];
    const profile = await getFulfillerPublicProfile(order.fulfiller_id);
    const history = await db.query("SELECT status, description, created_at as time FROM order_status_history WHERE order_id = $1 ORDER BY created_at ASC", [orderId]);
    res.status(200).json({ ...order, tracking_url: `https://api.awa.name.ng/track/${order.tracking_token}`, fulfiller_profile: profile, history: history.rows });
  } catch (error) {
    res.status(500).json({ error: 'Fetch failed' });
  }
};

/**
 * Returns user orders.
 */
const getUserOrders = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(`SELECT id, status, total_fare, pickup_address, delivery_address, created_at FROM orders WHERE user_id = $1 ORDER BY created_at DESC`, [userId]);
    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ error: 'Fetch failed' });
  }
};

/**
 * Updates an order status.
 */
const updateOrderStatus = async (req, res) => {
  const { orderId } = req.params;
  const { status } = req.body;
  try {
    await db.query("UPDATE orders SET status = $1 WHERE id = $2", [status, orderId]);
    await logStatusChange(db, orderId, status, `Mission state updated to ${status}`);
    res.status(200).json({ status });
  } catch (error) {
    res.status(500).json({ error: 'Update failed' });
  }
};

/**
 * Fetches order-scoped messages (Prompt 1).
 */
const getMessages = async (req, res) => {
  const { orderId } = req.params;
  const userId = req.user.id;

  try {
    // Auth: Must be participant
    const { rows: order } = await db.query("SELECT user_id, fulfiller_id FROM orders WHERE id = $1", [orderId]);
    if (order.length === 0) return res.status(404).json({ error: 'Order not found' });

    const isParticipant = order[0].user_id === userId || (order[0].fulfiller_id && order[0].fulfiller_id === req.user.fulfillerId);
    if (!isParticipant) return res.status(403).json({ error: 'Unauthorized' });

    const { rows } = await db.query(
      "SELECT * FROM messages WHERE order_id = $1 ORDER BY created_at ASC LIMIT 100",
      [orderId]
    );
    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ error: 'Failed to load messages' });
  }
};

/**
 * Files an incident report (Prompt 6).
 */
const fileIncident = async (req, res) => {
  const { orderId } = req.params;
  const { category, description, resolution_requested } = req.body;
  const fulfillerId = req.user.fulfillerId;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Create Dispute/Incident Record
    const disputeRes = await client.query(
      `INSERT INTO disputes (order_id, reporter_id, reason, status, category)
       VALUES ($1, (SELECT user_id FROM fulfillers WHERE id = $2), $3, 'OPEN', $4)
       RETURNING id`,
      [orderId, fulfillerId, description, `incident_${category}`]
    );
    const disputeId = disputeRes.rows[0].id;

    if (resolution_requested === 'handoff') {
        // HANDOFF path: Reset to SEARCHING
        await client.query(
            "UPDATE orders SET status = 'SEARCHING', fulfiller_id = NULL, incident_dispute_id = $1 WHERE id = $2",
            [disputeId, orderId]
        );
        await logStatusChange(client, orderId, 'SEARCHING', `Fulfiller handoff: ${description}`);
    } else if (resolution_requested === 'cancel_with_waiver_request') {
        // CANCEL path: Charge 25% immediately
        const orderRes = await client.query("SELECT total_fare FROM orders WHERE id = $1", [orderId]);
        const fare = orderRes.rows[0].total_fare;
        const fee = (fare * 0.25).toFixed(2);

        // Logic for auto-waive (security_risk)
        let waived = false;
        if (category === 'security_risk') waived = true;

        await client.query(
            "UPDATE orders SET status = 'CANCELLED', incident_dispute_id = $1, cancellation_fee_waived = $2 WHERE id = $3",
            [disputeId, waived, orderId]
        );

        // Record fee if not waived
        if (!waived) {
            await client.query(
                "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ((SELECT id FROM wallets WHERE owner_type = 'PLATFORM'), $1, 'CREDIT', 'CANCELLATION_FEE', $2)",
                [fee, orderId]
            );
        }

        await logStatusChange(client, orderId, 'CANCELLED', `Fulfiller cancelled: ${description}`);
    }

    await client.query('COMMIT');
    res.status(200).json({ message: 'Incident reported successfully' });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error("Incident Error:", error);
    res.status(500).json({ error: 'Failed to file incident' });
  } finally {
    client.release();
  }
};

/**
 * Fetches eligible queue candidates for a fulfiller (Prompt 7).
 */
const getQueueCandidates = async (req, res) => {
    const fulfillerId = req.user.fulfillerId;

    try {
        // 1. Eligibility Check: Must have an active order past pickup
        const activeRes = await db.query(
            "SELECT id, status, ST_X(delivery_location::geometry) as lng, ST_Y(delivery_location::geometry) as lat FROM orders WHERE fulfiller_id = $1 AND status IN ('PICKED_UP', 'EN_ROUTE_TO_DELIVERY', 'ARRIVED_AT_DELIVERY') LIMIT 1",
            [fulfillerId]
        );

        if (activeRes.rows.length === 0) return res.status(200).json([]); // Ineligible, return empty

        const activeOrder = activeRes.rows[0];

        // 2. Queue Slot Check: Must not already have a queued order
        const queueCheck = await db.query("SELECT id FROM orders WHERE queued_for_fulfiller_id = $1", [fulfillerId]);
        if (queueCheck.rows.length > 0) return res.status(200).json([]); // Hard cap of 1

        // 3. Proximity Filter: Pickup within 3km of active order delivery point
        const { rows: candidates } = await db.query(`
            SELECT id, pickup_address, delivery_address, total_fare, item_photo_url, pickup_display_summary
            FROM orders
            WHERE status = 'SEARCHING'
            AND ST_DWithin(
                pickup_location,
                ST_SetSRID(ST_MakePoint($1, $2), 4326),
                3000
            )
            LIMIT 5
        `, [activeOrder.lng, activeOrder.lat]);

        res.status(200).json(candidates);
    } catch (error) {
        console.error("Queue candidates error:", error);
        res.status(500).json({ error: 'Failed to fetch queue' });
    }
};

/**
 * Claims an order into the queue slot (Prompt 7).
 */
const claimQueueOrder = async (req, res) => {
    const { id } = req.params;
    const fulfillerId = req.user.fulfillerId;

    try {
        // Eligibility check
        const activeRes = await db.query("SELECT id FROM orders WHERE fulfiller_id = $1 AND status IN ('PICKED_UP', 'EN_ROUTE_TO_DELIVERY', 'ARRIVED_AT_DELIVERY')", [fulfillerId]);
        if (activeRes.rows.length === 0) return res.status(400).json({ error: 'Not eligible to queue' });

        const queueCheck = await db.query("SELECT id FROM orders WHERE queued_for_fulfiller_id = $1", [fulfillerId]);
        if (queueCheck.rows.length > 0) return res.status(400).json({ error: 'Queue slot already filled' });

        await db.query(
            "UPDATE orders SET status = 'QUEUED', queued_for_fulfiller_id = $1 WHERE id = $2 AND status = 'SEARCHING'",
            [fulfillerId, id]
        );

        res.status(200).json({ message: 'Order queued. It will activate after your current delivery.' });
    } catch (error) {
        res.status(500).json({ error: 'Failed to claim queued order' });
    }
};

module.exports = {
  getQuote, createOrder, acceptOrder, updateOrderStatus,
  verifyPickup, verifyDelivery, getOrderDetails, getUserOrders,
  getFulfillerPublicProfile, getMessages,
/**
 * Handles order cancellation with logic for fees (Prompt 6).
 */
const cancelOrder = async (req, res) => {
  const { orderId } = req.params;
  const { reason } = req.body;
  const userId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get Order state
    const { rows } = await client.query("SELECT * FROM orders WHERE id = $1 FOR UPDATE", [orderId]);
    if (rows.length === 0) return res.status(404).json({ error: 'Order not found' });
    const order = rows[0];

    // 2. Authorization
    const isCustomer = order.user_id === userId;
    const isFulfiller = order.fulfiller_id && (req.user.fulfillerId === order.fulfiller_id);
    if (!isCustomer && !isFulfiller) return res.status(403).json({ error: 'Unauthorized' });

    // 3. Logic Gating
    if (isFulfiller && order.status !== 'SEARCHING') {
        return res.status(400).json({ error: 'Fulfillers must file an incident report to cancel active orders.' });
    }

    let nextStatus = 'CANCELLED';
    if (isCustomer) {
        if (['MATCHED', 'EN_ROUTE_TO_PICKUP', 'ARRIVED_AT_PICKUP', 'PICKED_UP', 'EN_ROUTE_TO_DELIVERY', 'ARRIVED_AT_DELIVERY'].includes(order.status)) {
            // Apply 25% fee to platform
            const fee = (parseFloat(order.total_fare) * 0.25).toFixed(2);
            await client.query(
                "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ((SELECT id FROM wallets WHERE owner_type = 'PLATFORM'), $1, 'CREDIT', 'CANCELLATION_FEE', $2)",
                [fee, orderId]
            );
            nextStatus = 'CANCELLED_BY_USER';
        }
    }

    await client.query("UPDATE orders SET status = $1 WHERE id = $2", [nextStatus, orderId]);
    await logStatusChange(client, orderId, nextStatus, `Order cancelled by ${isCustomer ? 'Customer' : 'Fulfiller'}: ${reason}`);

    await client.query('COMMIT');
    res.status(200).json({ message: 'Order cancelled', status: nextStatus });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error("Cancel Error:", error);
    res.status(500).json({ error: 'Failed to cancel order' });
  } finally {
    client.release();
  }
};
  fileIncident,
  getQueueCandidates,
  claimQueueOrder
};
