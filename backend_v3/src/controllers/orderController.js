const db = require('../config/db');
const bcrypt = require('bcryptjs');
const geminiService = require('../services/geminiService');
const walletService = require('../services/walletService');
const emailService = require('../services/emailService');
const fcmService = require('../services/fcmService');
const smsService = require('../services/smsService');
const dispatchService = require('../services/dispatchService');
const PlatformConfig = require('../config/platform');
const { normalizePhone } = require('../utils/phone');

const BAD_WORDS = ['spam', 'test', 'nonsense', 'fake', 'dummy']; // Simplified V3 content check

/**
 * Generates a dynamic, distance-based quote.
 */
const getQuote = async (req, res) => {
  const {
    pickup_address, delivery_address, item_description,
    pickup_lat, pickup_lng, delivery_lat, delivery_lng,
    item_price = 0, initiator_role = 'PAYER', recipient_phone,
    pickup_state, pickup_landmark, delivery_landmark
  } = req.body;
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
  const sizeTier = aiResult.size_tier || 'MEDIUM';

  // 2.1 Determine Eligible Fulfiller Classes based on Size
  const sizeToClassMap = {
    'SMALL': ['agent', 'rider'],
    'MEDIUM': ['rider', 'driver'],
    'LARGE': ['driver']
  };
  const requiredClasses = sizeToClassMap[sizeTier] || ['rider', 'driver'];

  // 3. Apply Dynamic Pricing Dynamics (v3.5.1 Settings-Linked)
  let baseFees = { 'SMALL': 400, 'MEDIUM': 800, 'LARGE': 1500 };
  let perKmRate = 110;
  let roadWindingFactor = 1.15;
  let codFeeRate = 0.10; // Default 10%
  let trafficMultiplier = 1.0;
  let weatherMultiplier = 1.0;

  try {
    const settingsRes = await db.query("SELECT key, value FROM settings WHERE key IN ('base_fare_small', 'base_fare_medium', 'base_fare_large', 'per_km_rate', 'cod_fee_rate')");
    settingsRes.rows.forEach(r => {
        if (r.key === 'base_fare_small') baseFees['SMALL'] = parseFloat(r.value);
        if (r.key === 'base_fare_medium') baseFees['MEDIUM'] = parseFloat(r.value);
        if (r.key === 'base_fare_large') baseFees['LARGE'] = parseFloat(r.value);
        if (r.key === 'per_km_rate') perKmRate = parseFloat(r.value);
        if (r.key === 'cod_fee_rate') codFeeRate = parseFloat(r.value);
    });

    // 3.1 Check Weather Multiplier from Pickup Zone
    const zoneRes = await db.query(`
        SELECT id, weather_multiplier, is_dispatch_paused
        FROM zones
        WHERE ST_Intersects(ST_SetSRID(ST_MakePoint($1, $2), 4326)::geometry, boundary::geometry)
        LIMIT 1
    `, [pickup_lng, pickup_lat]);

    if (zoneRes.rows.length > 0) {
        if (zoneRes.rows[0].is_dispatch_paused) {
            return res.status(403).json({ success: false, message: 'Dispatch is temporarily paused in this zone due to severe conditions.' });
        }
        weatherMultiplier = parseFloat(zoneRes.rows[0].weather_multiplier || 1.0);
    }

    // 3.2 Check Traffic Corridor Multipliers
    // Logic: Find any active corridor that connects the pickup and delivery points
    const now = new Date();
    const day = now.getDay(); // 0-6
    const hour = now.getHours();
    const timeStr = `${hour}:${now.getMinutes().toString().padStart(2,'0')}`;

    const corridorRes = await db.query(`
        SELECT tc.time_windows
        FROM traffic_corridors tc
        WHERE tc.is_active = true
          AND ST_Intersects(ST_SetSRID(ST_MakePoint($1, $2), 4326)::geometry, (SELECT boundary::geometry FROM zones WHERE id = tc.pickup_zone_id))
          AND ST_Intersects(ST_SetSRID(ST_MakePoint($3, $4), 4326)::geometry, (SELECT boundary::geometry FROM zones WHERE id = tc.delivery_zone_id))
    `, [pickup_lng, pickup_lat, delivery_lng, delivery_lat]);

    corridorRes.rows.forEach(c => {
        const windows = c.time_windows || [];
        windows.forEach(w => {
            if (parseInt(w.day_of_week) === day) {
                if (timeStr >= w.start_time && timeStr <= w.end_time) {
                    trafficMultiplier = Math.max(trafficMultiplier, parseFloat(w.multiplier));
                }
            }
        });
    });

  } catch (e) {
    console.warn('[Quote] Pricing fetch failed, using fallback pricing.', e.message);
  }

  const base_fare = baseFees[aiResult.size_tier] || baseFees['MEDIUM'];
  const effectiveDistance = distanceKm * roadWindingFactor;

  // Apply Multipliers: (Base + Dist) * Weather * Traffic
  const raw_delivery_fee = (base_fare + (effectiveDistance * perKmRate)) * weatherMultiplier * trafficMultiplier;
  const delivery_fee = Math.ceil(raw_delivery_fee);

  // 4. Reliable Account Lookup (In-App vs Guest) - DETERMINES RECIPIENT TYPE
  let recipient_type = 'GUEST';
  let recipient_user_id = null;
  if (recipient_phone) {
      const normalized = normalizePhone(recipient_phone);
      const userMatch = await db.query("SELECT id FROM users WHERE phone = $1", [normalized]);
      if (userMatch.rows.length > 0) {
          recipient_type = 'APP_USER';
          recipient_user_id = userMatch.rows[0].id;
      }
  }

  // 5. Secure Pay / Escrow Fee Logic (DYNAMIZED)
  const platform_fee_amount = PlatformConfig.roundFee(item_price * codFeeRate);

  // FIXED RULE: The person paying for the item (Buyer) ALWAYS bears the fee.
  const fee_payer = 'PAYER';

  // 5.1 Guest SMS Charge (₦50)
  const sms_charge_amount = (recipient_type === 'GUEST') ? 50 : 0;

  // 5.2 Calculate UPFRONT Total (What the initiator pays NOW)
  // FIXED: For COD missions, Buyer pays EVERYTHING. Seller pays 0.
  const isPayerInitiator = initiator_role === 'PAYER';
  const isSecurePay = parseFloat(item_price) > 0;

  let total_payable;
  if (isSecurePay) {
      total_payable = isPayerInitiator ? (parseFloat(item_price) + platform_fee_amount + delivery_fee + sms_charge_amount) : 0;
  } else {
      // Non-COD: Initiator always pays delivery
      total_payable = delivery_fee + sms_charge_amount;
  }

  // 5.3 Calculate Recipient Total (For Seller-initiated COD)
  const recipient_total = (isSecurePay && !isPayerInitiator) ? (parseFloat(item_price) + platform_fee_amount + delivery_fee + sms_charge_amount) : 0;

  console.log(`[Quote] User: ${userId} | Item: ${item_price} | Upfront: ${total_payable} | Recipient Pays: ${recipient_total}`);

  // 6. Save Quote
    const quoteRes = await db.query(
    `INSERT INTO quotes (user_id, pickup_address, delivery_address, pickup_location, delivery_location, item_description, size_tier, total_fare, pickup_state, sms_charge_amount, required_fulfiller_classes, pickup_landmark, delivery_landmark)
     VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), ST_SetSRID(ST_MakePoint($6, $7), 4326), $8, $9, $10, $11, $12, $13, $14, $15)
     RETURNING id, expires_at`,
    [userId, pickup_address, delivery_address, pickup_lng, pickup_lat, delivery_lng, delivery_lat, item_description, sizeTier, total_payable, pickup_state, sms_charge_amount, requiredClasses, pickup_landmark, delivery_landmark]
  );

  res.status(200).json({
    success: true,
    quote_id: quoteRes.rows[0].id,
    size_tier: sizeTier,
    distance_km: distanceKm.toFixed(2),
    item_price,
    delivery_fee,
    platform_fee_amount,
    sms_charge_amount,
    weather_multiplier: weatherMultiplier,
    traffic_multiplier: trafficMultiplier,
    fee_payer,
    total_fare: total_payable,
    recipient_payable: recipient_total,
    required_fulfiller_classes: requiredClasses,
    payer_info: {
        type: recipient_type, // Map back to UI expectations
        user_id: recipient_user_id
    },
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

    if (rows[0].status !== 'SEARCHING' && rows[0].status !== 'PAYMENT_CAPTURED') {
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
  const userId = req.user.id;
  const userRole = req.user.role;

  // Ensure orderId is numeric to prevent SQL errors
  const numericId = parseInt(orderId);
  if (isNaN(numericId)) return res.status(400).json({ success: false, message: 'Invalid mission ID' });

  try {
    const { rows } = await db.query(
      `SELECT o.*,
       ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
       ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng,
       f.full_name as fulfiller_name, f.primary_class, f.mobility_type, f.profile_photo_url,
       f.tier, f.registration_number, f.rating_avg, f.rating_count, f.make,
       ST_Y(f.current_location::geometry) as fulfiller_lat, ST_X(f.current_location::geometry) as fulfiller_lng
       FROM orders o
       LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
       WHERE o.id = $1`,
      [numericId]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Mission not found' });

    const order = rows[0];

    // Map public profile for app
    order.fulfiller_profile = order.fulfiller_id ? {
        full_name: order.fulfiller_name,
        profile_photo_url: order.profile_photo_url,
        tier: order.tier,
        vehicle_registration_number: order.registration_number,
        make: order.make,
        mobility_type: order.mobility_type,
        primary_class: order.primary_class,
        rating_avg: parseFloat(order.rating_avg || 5.0),
        rating_count: parseInt(order.rating_count || 0),
        kyc_status: 'VERIFIED'
    } : null;

    // Security: Only return plain codes to the customer who created the order or admin
    // Cast user_id to string for reliable comparison
    if (order.user_id.toString() !== userId.toString() && userRole !== 'ADMIN') {
        delete order.pickup_code;
        delete order.delivery_code;
    }

    res.status(200).json(order);
  } catch (error) {
    console.error('[GetOrderDetails] FATAL:', error.message);
    res.status(500).json({ success: false, message: error.message });
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
                message: 'Collection required. This order has a mandatory Secure Pay amount that must be paid via app before delivery closure.'
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
        const { rows: orderData } = await db.query("SELECT item_price, escrow_status FROM orders WHERE id = $1", [orderId]);
        const isEscrow = orderData[0]?.item_price > 0 && orderData[0]?.escrow_status === 'held';

        if (!isEscrow && orderData[0]) {
            try {
                await walletService.processMissionSettlement(orderId);
            } catch (e) {
                console.error('[UpdateStatus] Settlement Error:', e.message);
            }
        }

        try {
            const { rows: oUser } = await db.query(
                "SELECT u.email, u.full_name, o.total_fare FROM orders o JOIN users u ON u.id = o.user_id WHERE o.id = $1",
                [orderId]
            );
            if (oUser.length > 0) {
                emailService.sendOrderCompletionEmail(oUser[0].email, oUser[0].full_name, orderId, oUser[0].total_fare)
                    .catch(e => console.error('[CompletionEmail] Error:', e.message));
            }
        } catch (e) {}
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
 * Policy: 25% fee if matched but before pickup. No cancellation after pickup.
 */
const cancelOrder = async (req, res) => {
    const { orderId } = req.params;
    const { reason } = req.body;
    const userId = req.user.id;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Lock and fetch current status
        const { rows } = await client.query(
            "SELECT id, status, total_fare, fulfiller_id FROM orders WHERE id = $1 AND user_id = $2 FOR UPDATE",
            [orderId, userId]
        );

        if (rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ success: false, message: 'Order not found' });
        }

        const order = rows[0];

        // 2. Policy Check: No cancellation after pickup
        const forbiddenStatuses = ['PICKED_UP', 'IN_TRANSIT', 'ARRIVED_AT_DELIVERY', 'DELIVERED', 'RELEASED', 'RECIPIENT_ABSENT'];
        if (forbiddenStatuses.includes(order.status)) {
            await client.query('ROLLBACK');
            return res.status(403).json({ success: false, message: 'Cancellation is strictly prohibited once the item has been picked up.' });
        }

        if (order.status === 'CANCELLED') {
            await client.query('ROLLBACK');
            return res.status(400).json({ success: false, message: 'Order is already cancelled' });
        }

        // 3. Penalty Logic: 25% if matched
        let penaltyApplied = 0;
        if (order.fulfiller_id) {
            penaltyApplied = parseFloat(order.total_fare) * 0.25;

            // Deduct from User Wallet
            const walletId = await walletService.ensureWalletExists(client, 'USER', userId);
            await walletService.recordEntry(client, walletId, 'DEBIT', penaltyApplied, 'CANCELLATION_PENALTY', `25% penalty for cancelling active mission #${orderId}`, orderId);

            // Note: In a future iteration, we can credit a portion of this to the agent.
            console.log(`[Cancel] Charged 25% penalty (₦${penaltyApplied}) to User ${userId} for matched Order ${orderId}`);
        }

        // 4. Update Status
        await client.query(
            "UPDATE orders SET status = 'CANCELLED', cancellation_reason = $1 WHERE id = $2",
            [reason || 'User cancelled', orderId]
        );

        await client.query('COMMIT');

        // Notify participants
        const socketService = require('../services/socketService');
        socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'CANCELLED' });

        res.status(200).json({
            success: true,
            message: penaltyApplied > 0 ? `Mission cancelled. A 25% penalty (₦${penaltyApplied.toFixed(2)}) was applied.` : 'Mission aborted'
        });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error(`[Cancel] Error for Order ${orderId}:`, error.message);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

/**
 * Initiates a return mission at 75% of the original fare.
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

        const returnFare = parseFloat(orig.total_fare) * 0.75; // Policy: 75% return fee

        // Create new mission with reversed addresses
        const returnRes = await db.query(
            `INSERT INTO orders (
                order_type, user_id, parent_order_id, status,
                pickup_address, delivery_address,
                pickup_location, delivery_location,
                total_fare, item_description, payment_status,
                original_total_fare, initiator_role
            ) VALUES (
                $1, $2, $3, 'SEARCHING',
                $4, $5, $6, $7, $8, $9, 'pending',
                $10, 'PAYER'
            ) RETURNING id`,
            [
                orig.order_type, userId, orig.id,
                orig.delivery_address, orig.pickup_address, // Reversed
                orig.delivery_location, orig.pickup_location, // Reversed
                returnFare, `RETURN: ${orig.item_description}`,
                orig.total_fare // Store original for audit
            ]
        );

        res.status(201).json({
            success: true,
            message: 'Return mission created. 75% return fee applied. Please complete payment.',
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
        const {
            quote_id, payment_method, recipient_name, recipient_phone, notes,
            pickup_display_summary, delivery_display_summary, item_photo_url,
            promo_id, payment_reference,
            pickup_lat, pickup_lng, delivery_lat, delivery_lng,
            item_price, delivery_fee, platform_fee_amount, sms_charge_amount, fee_payer, initiator_role,
            payer_id, pickup_state, recipient_payable
        } = req.body;
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

        // 3. Handle Promo/Coupon Server-Side Verification (Restricted to Delivery Fee)
        let deliveryFee = parseFloat(delivery_fee || q.delivery_fee || 0);
        const itemPriceNum = parseFloat(item_price || 0);
        const platformFeeNum = (initiator_role === 'PAYER') ? parseFloat(platform_fee_amount || 0) : 0;

        let discount = 0;
        let couponId = null;

        if (promo_id) {
            const couponRes = await client.query("SELECT * FROM coupons WHERE id = $1 AND is_active = true", [promo_id]);
            if (couponRes.rows.length > 0) {
                const c = couponRes.rows[0];
                couponId = c.id;
                const calculatedDiscount = c.discount_type === 'FIXED' ? parseFloat(c.discount_value) : deliveryFee * (parseFloat(c.discount_value) / 100);

                // Rule: Promo only discounts delivery fee, never item price or platform fee.
                discount = Math.min(calculatedDiscount, deliveryFee);
                deliveryFee = Math.max(0, deliveryFee - discount);

                console.log(`[Order] Applied Promo: ${c.code}. Discount: ${discount}. New Delivery Fee: ${deliveryFee}`);
            }
        }

        const finalFare = itemPriceNum + deliveryFee + platformFeeNum;

        // 4. Determine Initial Status
        // Rule: If receiver is an app user, require acknowledgment before fulfiller search.
        const isReceiverAppUser = q.payer_info?.type === 'APP_USER' || recipient_payable > 0; // Simplified check or based on quote
        const initialStatus = isReceiverAppUser ? 'PENDING_ACKNOWLEDGMENT' : (finalFare === 0 ? 'AWAITING_PAYMENT' : 'PAYMENT_CAPTURED');

        // 4.1 Create Order (DEFINITIVE ALIGNMENT WITH WEBHOOK)
        // Extract coordinates from body (preferred) or quote fallback
        const pLat = pickup_lat || 0;
        const pLng = pickup_lng || 0;
        const dLat = delivery_lat || 0;
        const dLng = delivery_lng || 0;

        const refToSave = payment_reference || `FREE_${q.id.substring(0,8)}_${Date.now()}`;
        console.log(`[ManualOrder] PRE-FLIGHT: Quote: ${q.id} | User: ${userId} | Fare: ${finalFare} | Ref: ${refToSave} | Status: ${initialStatus}`);

        const pCode = Math.floor(1000 + Math.random() * 9000).toString();
        const dCode = Math.floor(1000 + Math.random() * 9000).toString();
        const pHash = await bcrypt.hash(pCode, 10);
        const dHash = await bcrypt.hash(dCode, 10);

        const orderRes = await client.query(
            `INSERT INTO orders (
                order_type, user_id, quote_id, status, item_description, size_tier,
                pickup_address, delivery_address, pickup_location, delivery_location,
                total_fare, payment_status, payment_method, payment_reference, payment_channel,
                recipient_name, recipient_phone, notes, pickup_display_summary, delivery_display_summary, item_photo_url,
                pickup_code_hash, delivery_code_hash, pickup_code, delivery_code, coupon_id,
                item_price, delivery_fee, platform_fee_amount, fee_payer, initiator_role,
                escrow_status, payer_id, original_delivery_fee, original_total_fare, pickup_state,
                sms_charge_amount, required_fulfiller_classes, pickup_landmark, delivery_landmark,
                recipient_user_id
            ) VALUES (
                'pickup_delivery', $1, $2, $3, $4, $5, $6, $7,
                ST_SetSRID(ST_MakePoint($8, $9), 4326)::geography,
                ST_SetSRID(ST_MakePoint($10, $11), 4326)::geography,
                $12, $13, $14, $15, $16,
                $17, $18, $19, $20, $21, $22,
                $23, $24, $25, $26, $27::uuid,
                $28, $29, $30, $31, $32,
                $33, $34, $35, $36, $37,
                $38, $39, $40, $41,
                $42
            ) RETURNING id`,
            [
                userId, // $1
                q.id,   // $2
                initialStatus, // $3
                q.item_description, // $4
                q.size_tier, // $5
                q.pickup_address, // $6
                q.delivery_address, // $7
                pLng, // $8
                pLat, // $9
                dLng, // $10
                dLat, // $11
                finalFare, // $12
                finalFare === 0 ? 'pending' : 'PAID', // $13
                payment_method || 'card', // $14
                refToSave, // $15
                payment_method || 'card', // $16 (payment_channel)
                recipient_name || 'Recipient', // $17
                recipient_phone || '000', // $18
                notes || null, // $19
                pickup_display_summary || q.pickup_address.substring(0, 50), // $20
                delivery_display_summary || q.delivery_address.substring(0, 50), // $21
                item_photo_url || null, // $22
                pHash, // $23
                dHash, // $24
                pCode, // $25
                dCode, // $26
                couponId || null, // $27
                parseFloat(item_price || 0), // $28
                parseFloat(deliveryFee || 0), // $29
                parseFloat(platformFeeNum || 0), // $30
                fee_payer || 'PAYER', // $31
                initiator_role || 'PAYER', // $32
                (parseFloat(item_price || 0) > 0) ? 'held' : 'not_applicable', // $33
                payer_id || null, // $34
                parseFloat(q.delivery_fee), // $35
                parseFloat(q.total_fare), // $36
                pickup_state || q.pickup_state, // $37
                parseFloat(sms_charge_amount || 0), // $38
                q.required_fulfiller_classes, // $39
                q.pickup_landmark, // $40
                q.delivery_landmark, // $41
                q.payer_info?.user_id // $42 (recipient_user_id)
            ]
        );

        await client.query('COMMIT');
        console.log(`[ManualOrder] Mission activated: ${orderRes.rows[0].id} for User: ${userId} | Status: ${initialStatus}`);

        // 5. In-App Outreach (Skip SMS if App User)
        if (initialStatus === 'PENDING_ACKNOWLEDGMENT' && q.payer_info?.user_id) {
            fcmService.sendNotification(
                q.payer_info.user_id,
                "New Delivery for You! 📦",
                `${q.initiator_name || 'A user'} wants to send you an item. Tap to confirm your delivery address.`,
                { type: "ACKNOWLEDGMENT_REQUEST", order_id: orderRes.rows[0].id.toString() }
            );
        } else {
            // 5.1 Active Dispatch (Immediate for Guest/Prepaid)
            if (orderRes.rows[0].status === 'SEARCHING' || orderRes.rows[0].status === 'PAYMENT_CAPTURED') {
                const fulfillers = await dispatchService.findNearbyFulfillers(orderRes.rows[0]);
                if (fulfillers.length > 0) {
                    dispatchService.broadcastOffer(orderRes.rows[0], fulfillers).catch(() => {});
                }
            }

            // 5.2 Outreach for Secure Pay (Guest)
            if (item_price > 0) {
                if (payer_id) {
                    fcmService.sendNotification(payer_id, "Secure Pay Request", `A Secure Pay request for ₦${recipient_payable || item_price} is waiting for your payment.`, { type: "SECURE_PAY_REQUEST", order_id: orderRes.rows[0].id.toString() });
                } else if (recipient_phone && recipient_type === 'GUEST') {
                    smsService.sendSecurePaySms(recipient_phone, recipient_payable || item_price, orderRes.rows[0].id).catch(e => {});
                }
            }
        }

        // Send Payment Receipt Email
        try {
            const { rows: uRes } = await client.query("SELECT email, full_name FROM users WHERE id = $1", [userId]);
            if (uRes.length > 0) {
                emailService.sendPaymentReceiptEmail(uRes[0].email, uRes[0].full_name, orderRes.rows[0].id, finalFare, q.item_description)
                    .catch(e => console.error('[ReceiptEmail] Error:', e.message));
            }
        } catch (e) {}

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

/**
 * Verifies the 4-digit pickup code.
 * Supports Universal Test Code: '8888', '1234', '0000', '9999'.
 */
const verifyPickup = async (req, res) => {
    const id = req.params.orderId || req.params.id;
    const { code } = req.body;
    const masterOtp = process.env.MASTER_OTP || '8888';
    const universalCodes = [masterOtp.toString(), '8888', '1234', '0000', '9999'];

    try {
        const { rows } = await db.query(
            "SELECT id, status, pickup_code_hash FROM orders WHERE id = $1",
            [id]
        );

        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: 'Order not found' });
        }

        const order = rows[0];

        // Universal Test Code Check
        const isMaster = universalCodes.includes((code || '').toString().trim());

        let isValid = isMaster;
        if (!isValid && order.pickup_code_hash && order.pickup_code_hash !== 'v3_pending') {
            isValid = await bcrypt.compare(code.toString(), order.pickup_code_hash);
        } else if (!isValid && order.pickup_code_hash === 'v3_pending') {
            isValid = true;
        }

        if (!isValid) {
            return res.status(400).json({ success: false, message: 'Invalid 4-digit pickup code' });
        }

        // Update status to PICKED_UP
        await db.query(
            "UPDATE orders SET status = 'PICKED_UP', picked_up_at = CURRENT_TIMESTAMP WHERE id = $1",
            [id]
        );

        // Socket notify
        try {
            const socketService = require('../services/socketService');
            socketService.getIO().to(`order_${id}`).emit("status_updated", { orderId: id, status: 'PICKED_UP' });
        } catch (e) {}

        console.log(`[Order] Pickup verified for Mission #${id} using code: ${code}`);

        res.status(200).json({
            success: true,
            status: 'PICKED_UP',
            message: 'Pickup code verified successfully'
        });

        // 3. Trigger Guest Tracking SMS (Termii)
        try {
            const { rows } = await db.query(
                "SELECT recipient_phone FROM orders WHERE id = $1",
                [id]
            );
            if (rows.length > 0 && rows[0].recipient_phone) {
                const phone = rows[0].recipient_phone;
                const normalized = normalizePhone(phone);
                const userCheck = await db.query("SELECT id FROM users WHERE phone = $1", [normalized]);

                // If recipient is NOT an app user, send tracking SMS
                if (userCheck.rows.length === 0) {
                    await smsService.sendTrackingLinkSms(phone, id);
                }
            }
        } catch (e) {
            console.error('[VerifyPickup] Guest SMS fail:', e.message);
        }

    } catch (error) {
        console.error('[VerifyPickup] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Verifies the 4-digit delivery code and completes mission.
 * Supports Universal Test Code: '8888', '1234', '0000', '9999'.
 */
const verifyDelivery = async (req, res) => {
    const id = req.params.orderId || req.params.id;
    const { code, delivery_photo_url } = req.body;
    const masterOtp = process.env.MASTER_OTP || '8888';
    const universalCodes = [masterOtp.toString(), '8888', '1234', '0000', '9999'];

    console.log(`[VerifyDelivery] Mission #${id} completion request with code: ${code}`);

    try {
        const { rows } = await db.query(
            "SELECT id, status, delivery_code_hash, user_id, item_price, escrow_status FROM orders WHERE id = $1",
            [id]
        );

        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: 'Order not found in database' });
        }

        const order = rows[0];
        const isEscrow = parseFloat(order.item_price || 0) > 0 && order.escrow_status === 'held';

        // Universal Test Code Check
        const isMaster = universalCodes.includes((code || '').toString().trim());

        let isValid = isMaster;
        if (!isValid && order.delivery_code_hash && order.delivery_code_hash !== 'v3_pending') {
            isValid = await bcrypt.compare(code.toString(), order.delivery_code_hash);
        } else if (!isValid && order.delivery_code_hash === 'v3_pending') {
            isValid = true;
        }

        if (!isValid) {
            return res.status(400).json({ success: false, message: 'Invalid 4-digit delivery code' });
        }

        // Update status. If escrow, it's pending buyer confirmation.
        const nextStatus = isEscrow ? 'DELIVERED_PENDING_CONFIRMATION' : 'DELIVERED';

        await db.query(
            "UPDATE orders SET status = $1, delivered_at = CURRENT_TIMESTAMP, pod_photo_url = $2 WHERE id = $3",
            [nextStatus, delivery_photo_url || null, id]
        );

        // Set grace period if escrow
        if (isEscrow) {
            const graceHours = parseInt(PlatformConfig.ESCROW.GRACE_PERIOD_HOURS || 48);
            await db.query(
                "UPDATE orders SET grace_period_expires_at = CURRENT_TIMESTAMP + ($1 || ' hours')::interval WHERE id = $2",
                [graceHours, id]
            );

            // Notify Buyer to Confirm (FCM)
            fcmService.sendSecurePayReminder(order.user_id, id).catch(e => console.error('[FCM] Reminder Error:', e.message));
        }

        // Trigger Settlement for Delivery Fee portion
        try {
            const walletService = require('../services/walletService');
            await walletService.processMissionSettlement(id);
        } catch (e) {
            console.error('[VerifyDelivery] Wallet Settlement Warning:', e.message);
        }

        // Trigger Order Completion Email
        try {
            const emailService = require('../services/emailService');
            const { rows: oUser } = await db.query(
                "SELECT u.email, u.full_name, o.total_fare FROM orders o JOIN users u ON u.id = o.user_id WHERE o.id = $1",
                [id]
            );
            if (oUser.length > 0) {
                emailService.sendOrderCompletionEmail(oUser[0].email, oUser[0].full_name, id, oUser[0].total_fare)
                    .catch(err => console.error('[CompletionEmail] Error:', err.message));
            }
        } catch (e) {}

        // Socket notify
        try {
            const socketService = require('../services/socketService');
            socketService.getIO().to(`order_${id}`).emit("status_updated", { orderId: id, status: 'DELIVERED' });
        } catch (e) {}

        console.log(`[Order] Delivery verified for Mission #${id} using code: ${code}`);

        res.status(200).json({
            success: true,
            status: 'DELIVERED',
            message: 'Delivery verified successfully'
        });

    } catch (error) {
        console.error('[VerifyDelivery] FATAL ERROR:', error);
        res.status(500).json({
            success: false,
            message: `Internal Error: ${error.message}`,
            stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
};


/**
 * Buyer confirms receipt of item, releasing escrow to seller.
 */
const confirmReceipt = async (req, res) => {
    const { orderId } = req.params;
    const userId = req.user.id;

    try {
        const { rows } = await db.query(
            "SELECT id, status, user_id FROM orders WHERE id = $1",
            [orderId]
        );

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Order not found' });
        const order = rows[0];

        if (order.user_id !== userId) return res.status(403).json({ success: false, message: 'Unauthorized' });
        if (order.status !== 'DELIVERED_PENDING_CONFIRMATION') {
            return res.status(400).json({ success: false, message: 'Order is not in a state that can be confirmed' });
        }

        // Release Escrow
        await walletService.releaseEscrow(orderId);

        // Notify participants
        const socketService = require('../services/socketService');
        socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'RELEASED' });

        res.status(200).json({ success: true, message: 'Payment released to seller' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Buyer reports a problem, moving order to DISPUTED status and blocking auto-release.
 */
const reportProblem = async (req, res) => {
    const { orderId } = req.params;
    const { reason, notes } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query(
            "UPDATE orders SET status = 'DISPUTED', escrow_status = 'disputed' WHERE id = $1 AND user_id = $2 AND status = 'DELIVERED_PENDING_CONFIRMATION' RETURNING id",
            [orderId, userId]
        );

        if (rows.length === 0) return res.status(400).json({ success: false, message: 'Could not dispute order' });

        // Record Dispute
        await db.query(
            "INSERT INTO disputes (order_id, reporter_id, reason, status) VALUES ($1, $2, $3, 'OPEN')",
            [orderId, userId, `${reason}: ${notes}`]
        );

        // Notify participants
        const socketService = require('../services/socketService');
        socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'DISPUTED' });

        res.status(200).json({ success: true, message: 'Dispute filed. Admin will review.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Allows a customer to rate a fulfiller after delivery.
 */
const rateFulfiller = async (req, res) => {
    const { orderId } = req.params;
    const { rating, comment } = req.body;
    const userId = req.user.id;

    try {
        // 1. Verify order exists and belongs to user
        const { rows } = await db.query(
            "SELECT id, fulfiller_id, status, customer_rating FROM orders WHERE id = $1 AND user_id = $2",
            [orderId, userId]
        );

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Order not found' });
        const order = rows[0];

        if (order.customer_rating !== null) {
            return res.status(400).json({ success: false, message: "You've already rated this mission." });
        }

        if (!order.fulfiller_id) return res.status(400).json({ success: false, message: 'No fulfiller assigned to this order' });

        const validStatuses = ['DELIVERED', 'RELEASED', 'DELIVERED_PENDING_CONFIRMATION', 'CONFIRMED'];
        if (!validStatuses.includes(order.status.toUpperCase())) {
            return res.status(400).json({ success: false, message: 'Mission must be completed before rating.' });
        }

        // 2. Record rating in order
        await db.query(
            "UPDATE orders SET customer_rating = $1, customer_comment = $2 WHERE id = $3",
            [rating, comment, orderId]
        );

        // 3. Recalculate Fulfiller Avg Rating
        await db.query(`
            UPDATE fulfillers
            SET rating_avg = (
                SELECT COALESCE(AVG(customer_rating), 5.0)::decimal(3,2)
                FROM orders
                WHERE fulfiller_id = $1 AND customer_rating IS NOT NULL
            ),
            rating_count = (
                SELECT COUNT(*)::integer
                FROM orders
                WHERE fulfiller_id = $1 AND customer_rating IS NOT NULL
            )
            WHERE id = $1
        `, [order.fulfiller_id]);

        res.status(200).json({ success: true, message: 'Thank you for your feedback!' });
    } catch (error) {
        console.error('[RateFulfiller] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Allows a fulfiller to rate a customer after delivery.
 */
const rateCustomer = async (req, res) => {
    const { orderId } = req.params;
    const { rating, comment } = req.body;
    const userId = req.user.id;

    try {
        // 1. Verify order exists and belongs to this fulfiller (via fulfillers table join)
        const { rows } = await db.query(`
            SELECT o.id, o.fulfiller_id, o.status, o.fulfiller_rating
            FROM orders o
            JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE o.id = $1 AND f.user_id = $2
        `, [orderId, userId]);

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Order not found or unauthorized' });
        const order = rows[0];

        if (order.fulfiller_rating !== null) {
            return res.status(400).json({ success: false, message: "You've already rated this customer." });
        }

        const validStatuses = ['DELIVERED', 'RELEASED', 'DELIVERED_PENDING_CONFIRMATION', 'CONFIRMED'];
        if (!validStatuses.includes(order.status.toUpperCase())) {
            return res.status(400).json({ success: false, message: 'Mission must be completed before rating.' });
        }

        // 2. Record rating in order
        await db.query(
            "UPDATE orders SET fulfiller_rating = $1, fulfiller_comment = $2 WHERE id = $3",
            [rating, comment, orderId]
        );

        res.status(200).json({ success: true, message: 'Customer rated successfully.' });
    } catch (error) {
        console.error('[RateCustomer] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns missions assigned to or completed by a fulfiller.
 */
const getFulfillerOrders = async (req, res) => {
    const userId = req.user.id;
    const { filter = 'all' } = req.query;

    try {
        // 1. Fetch fulfiller profile to get the internal DB ID
        // Robust check: Ensure userId is treated as integer
        const { rows: fulfiller } = await db.query("SELECT id FROM fulfillers WHERE user_id = $1::integer", [userId]);
        if (fulfiller.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller profile not found' });

        const fId = fulfiller[0].id;

        // 2. Prepare Status Filter
        let statusFilter = "";
        const f = filter.toLowerCase();
        if (f === 'active') {
            statusFilter = "AND o.status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED')";
        } else if (f === 'completed') {
            statusFilter = "AND o.status IN ('DELIVERED', 'RELEASED')";
        }

        // 3. Query all missions for this agent
        // Calculation: 75% share for agent (from the original delivery fee)
        const { rows } = await db.query(
            `SELECT o.*,
             ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
             ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng,
             ROUND(COALESCE(o.original_delivery_fee, o.delivery_fee, o.total_fare) * 0.75, 2) as earnings
             FROM orders o
             WHERE (o.fulfiller_id = $1 OR o.queued_for_fulfiller_id = $1)
             ${statusFilter}
             ORDER BY o.created_at DESC`,
            [fId]
        );

        console.log(`[FulfillerOrders] Agent ${fId} (User: ${userId}) | Filter: ${filter} | Found: ${rows.length}`);

        res.status(200).json(rows);
    } catch (error) {
        console.error('[FulfillerOrders] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Public endpoint for Guest Live Tracking.
 */
const getGuestTracking = async (req, res) => {
    const { orderId } = req.params;
    try {
        const { rows } = await db.query(`
            SELECT o.*,
            ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
            ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng,
            f.full_name as fulfiller_name, f.mobility_type, f.rating_avg,
            ST_Y(f.current_location::geometry) as fulfiller_lat, ST_X(f.current_location::geometry) as fulfiller_lng
            FROM orders o
            LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE o.id = $1`, [orderId]);

        if (rows.length === 0) return res.status(404).send("Mission not found.");

        res.render('guest_tracking', { order: rows[0], layout: false });
    } catch (e) {
        res.status(500).send("Tracking unavailable.");
    }
};

/**
 * Internal Helper: Processes crowdsourced landmarks.
 */
const processLandmark = async (text, lat, lng) => {
    if (!text || text.length < 3) return;

    const normalized = text.toLowerCase().trim();

    // 1. Lightweight Content Check
    const isBad = BAD_WORDS.some(word => normalized.includes(word));
    if (isBad) {
        console.warn(`[Landmark] Blocked suspicious entry: ${text}`);
        return;
    }

    try {
        // 2. Proximity Check (~200m) and Upsert
        // We use ST_DWithin on geometry to find a matching normalized name within 200m.
        const existing = await db.query(`
            SELECT id FROM landmark_suggestions
            WHERE normalized_text = $1
              AND ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint($2, $3), 4326)::geography, 200)
            LIMIT 1
        `, [normalized, lng, lat]);

        if (existing.rows.length > 0) {
            await db.query("UPDATE landmark_suggestions SET submission_count = submission_count + 1 WHERE id = $1", [existing.rows[0].id]);
        } else {
            await db.query(`
                INSERT INTO landmark_suggestions (normalized_text, display_text, location, status)
                VALUES ($1, $2, ST_SetSRID(ST_MakePoint($3, $4), 4326), 'approved')
            `, [normalized, text.trim(), lng, lat]);
        }
    } catch (e) {
        console.error('[Landmark] Process error:', e.message);
    }
};

/**
 * Fulfiller marks delivery as failed after 10-minute timeout.
 */
const failDelivery = async (req, res) => {
    const { orderId } = req.params;
    const { reason, evidence_photo_url } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query(`
            SELECT o.* FROM orders o
            JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE o.id = $1 AND f.user_id = $2 AND o.status = 'ARRIVED_AT_DELIVERY'
        `, [orderId, userId]);

        if (rows.length === 0) return res.status(400).json({ success: false, message: 'Invalid order state for failure marking.' });

        await db.query(
            "UPDATE orders SET status = 'RECIPIENT_ABSENT', pod_photo_url = $1, cancellation_reason = $2 WHERE id = $3",
            [evidence_photo_url || null, reason || 'Recipient Unavailable', orderId]
        );

        await walletService.processMissionSettlement(orderId);

        const socketService = require('../services/socketService');
        socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'RECIPIENT_ABSENT' });

        res.status(200).json({ success: true, message: 'Mission marked as failed. Standard settlement applied.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Fulfiller requests delivery consent (leave at door/third party).
 */
const requestConsent = async (req, res) => {
    const { orderId } = req.params;
    const { note } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query(`
            SELECT o.* FROM orders o
            JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE o.id = $1 AND f.user_id = $2
        `, [orderId, userId]);

        if (rows.length === 0) return res.status(400).json({ success: false, message: 'Order not found or unauthorized.' });
        const order = rows[0];

        // Send SMS to recipient with consent link
        const consentUrl = `https://track.pikop.com.ng/api/v1/orders/consent/${order.id}`;
        const message = `Pikop: Our agent requested your consent to complete delivery (${note}). Approve here: ${consentUrl}`;

        await smsService.sendSms(order.recipient_phone, message, 'delivery_consent', order.id);

        res.status(200).json({ success: true, message: 'Consent request sent to recipient.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Public endpoint to view and grant delivery consent.
 */
const getConsentPage = async (req, res) => {
    const { orderId } = req.params;
    try {
        const { rows } = await db.query("SELECT * FROM orders WHERE id = $1", [orderId]);
        if (rows.length === 0) return res.status(404).send("Mission not found.");
        res.render('delivery_consent', { order: rows[0], layout: false });
    } catch (e) {
        res.status(500).send("Consent service unavailable.");
    }
};

/**
 * Public endpoint to grant delivery consent.
 */
const grantConsent = async (req, res) => {
    const { orderId } = req.params;
    try {
        await db.query(
            "UPDATE orders SET status = 'DELIVERED', escrow_status = 'released', matched_at = NOW() WHERE id = $1",
            [orderId]
        );
        // Standard settlement
        await walletService.processMissionSettlement(orderId);

        const socketService = require('../services/socketService');
        socketService.getIO().to(`order_${orderId}`).emit("status_updated", { orderId, status: 'DELIVERED' });

        res.status(200).json({ success: true, message: 'Consent granted. Delivery completed.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Public endpoint to view order breakdown and pay (Secure Pay Recipient).
 */
const getGuestCheckout = async (req, res) => {
    const { orderId } = req.params;
    try {
        const { rows } = await db.query(`
            SELECT o.*,
            u.full_name as initiator_name
            FROM orders o
            JOIN users u ON u.id = o.user_id
            WHERE o.id = $1`, [orderId]);

        if (rows.length === 0) return res.status(404).send("Mission not found.");
        const order = rows[0];

        if (order.payment_status === 'PAID') {
            return res.send(`
                <html>
                    <body style="font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; text-align: center;">
                        <div>
                            <h2 style="color: #008751;">Payment Already Completed</h2>
                            <p>This mission has already been activated. Thank you!</p>
                        </div>
                    </body>
                </html>
            `);
        }

        // Calculate breakdown for display
        const itemTotal = parseFloat(order.item_price) + parseFloat(order.platform_fee_amount);
        const logisticsTotal = parseFloat(order.delivery_fee) + parseFloat(order.sms_charge_amount);
        const grandTotal = itemTotal + logisticsTotal;

        res.render('guest_checkout', {
            order,
            itemTotal,
            logisticsTotal,
            grandTotal,
            layout: false
        });
    } catch (e) {
        res.status(500).send("Checkout unavailable.");
    }
};

/**
 * Receiver acknowledges an incoming mission.
 */
const acknowledgeOrder = async (req, res) => {
    const { orderId } = req.params;
    const { action, corrected_address, lat, lng } = req.body;
    const userId = req.user.id;

    try {
        const { rows } = await db.query(
            "SELECT id, status, user_id, recipient_user_id FROM orders WHERE id = $1 AND recipient_user_id = $2 FOR UPDATE",
            [orderId, userId]
        );

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Incoming delivery not found' });
        const order = rows[0];

        if (order.status !== 'PENDING_ACKNOWLEDGMENT') {
            return res.status(400).json({ success: false, message: 'Order is not awaiting acknowledgment' });
        }

        if (action === 'decline') {
            await db.query("UPDATE orders SET status = 'CANCELLED', cancellation_reason = 'Receiver declined' WHERE id = $1", [orderId]);
            fcmService.sendNotification(order.user_id, "Delivery Declined", "The receiver has declined your delivery request. Any prepaid funds will be credited to your wallet.", { type: "ORDER_UPDATE", order_id: orderId.toString() });
            return res.status(200).json({ success: true, message: 'Delivery declined' });
        }

        // Action: Confirm
        const updateQuery = `
            UPDATE orders
            SET status = 'SEARCHING',
                delivery_address = COALESCE($1, delivery_address),
                delivery_location = CASE WHEN $2 IS NOT NULL THEN ST_SetSRID(ST_MakePoint($3, $2), 4326)::geography ELSE delivery_location END
            WHERE id = $4
        `;
        await db.query(updateQuery, [corrected_address || null, lat || null, lng || null, orderId]);

        // Start Fulfiller Search
        const updatedOrder = (await db.query("SELECT * FROM orders WHERE id = $1", [orderId])).rows[0];
        const fulfillers = await dispatchService.findNearbyFulfillers(updatedOrder);
        if (fulfillers.length > 0) {
            dispatchService.broadcastOffer(updatedOrder, fulfillers).catch(() => {});
        }

        fcmService.sendNotification(order.user_id, "Delivery Acknowledged!", "The receiver has confirmed the delivery. We are now matching an agent.", { type: "ORDER_UPDATE", order_id: orderId.toString() });

        res.status(200).json({ success: true, message: 'Delivery acknowledged and dispatched' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Sender decides next steps after acknowledgment timeout.
 */
const handleAcknowledgmentTimeoutChoice = async (req, res) => {
    const { orderId } = req.params;
    const { choice } = req.body; // proceed, cancel
    const userId = req.user.id;

    try {
        const { rows } = await db.query(
            "SELECT id, status, user_id FROM orders WHERE id = $1 AND user_id = $2 FOR UPDATE",
            [orderId, userId]
        );

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Mission not found' });
        const order = rows[0];

        if (order.status !== 'PENDING_ACKNOWLEDGMENT') return res.status(400).json({ success: false, message: 'Mission is no longer in timeout state' });

        if (choice === 'cancel') {
            await db.query("UPDATE orders SET status = 'CANCELLED', cancellation_reason = 'Timeout - Sender cancelled' WHERE id = $1", [orderId]);
            return res.status(200).json({ success: true, message: 'Mission cancelled' });
        }

        // Choice: Proceed
        await db.query("UPDATE orders SET status = 'SEARCHING' WHERE id = $1", [orderId]);

        const updatedOrder = (await db.query("SELECT * FROM orders WHERE id = $1", [orderId])).rows[0];
        const fulfillers = await dispatchService.findNearbyFulfillers(updatedOrder);
        if (fulfillers.length > 0) {
            dispatchService.broadcastOffer(updatedOrder, fulfillers).catch(() => {});
        }

        res.status(200).json({ success: true, message: 'Mission dispatched' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
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
  getFulfillerOrders,
  acknowledgeOrder,
  handleAcknowledgmentTimeoutChoice,
  cancelOrder,
  verifyPickup,
  verifyDelivery,
  confirmReceipt,
  reportProblem,
  rateFulfiller,
  rateCustomer,
  getGuestTracking,
  processLandmark,
  failDelivery,
  requestConsent,
  getConsentPage,
  grantConsent,
  getGuestCheckout
};
