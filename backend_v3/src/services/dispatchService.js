const db = require('../config/db');
const socketService = require('./socketService');
const fcmService = require('./fcmService');
const { getWATTimeStr, isWithinWindow } = require('../utils/time');

/**
 * Shared Dispatch Engine - v3 (Milestone 6)
 */
const findNearbyFulfillers = async (order, radiusOverride = null, includeFleets = false) => {
  const radiusMeters = radiusOverride || 30000; // Default 30km for instant broad reach

  try {
    const fleetCondition = includeFleets ? "" : "AND f.fleet_partner_id IS NULL";

    // Resilient Fulfiller Search Query:
    // Matches online verified fulfillers within state or distance, handling NULLs gracefully
    const query = `
      SELECT f.user_id, f.id, f.primary_class, COALESCE(ST_Distance(f.current_location, $1), 0) as dist,
             (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) as load
      FROM fulfillers f
      LEFT JOIN zones z ON (f.current_location IS NOT NULL AND ST_Intersects(f.current_location::geometry, z.boundary::geometry))
      WHERE f.online_status = 'ONLINE'
      AND f.kyc_status = 'VERIFIED'
      AND (f.current_state IS NULL OR f.current_state = '' OR f.current_state ILIKE $3 OR $5 ILIKE '%' || f.current_state || '%')
      AND (f.current_location IS NULL OR ST_DWithin(f.current_location, $1, $2))
      ${fleetCondition}

      -- 1. Class-based Eligibility (from Order Size)
      AND (f.primary_class = ANY($4::text[]) OR cardinality($4::text[]) = 0)

      -- 2. Zone-based Restrictions
      AND (z.id IS NULL OR f.primary_class = ANY(z.allowed_fulfiller_classes))

      AND (
          (f.primary_class = 'agent' AND (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) < 2)
          OR (f.primary_class = 'rider' AND (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) < 5)
          OR (f.primary_class = 'driver' AND (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) < 15)
          OR (f.primary_class NOT IN ('agent', 'rider', 'driver'))
      )
      ORDER BY dist ASC
      LIMIT 25
    `;

    const rawState = order.pickup_state || order.pickup_address || '';
    const statePattern = `%${rawState.split(',')[0].trim().split(' ')[0]}%`;

    let restrictedClasses = order.required_fulfiller_classes || ['rider', 'driver', 'foot_agent', 'agent'];
    try {
        const settingsRes = await db.query("SELECT key, value FROM settings WHERE key IN ('daylight_dispatch_start', 'daylight_dispatch_end')");
        const settings = {};
        settingsRes.rows.forEach(r => settings[r.key] = r.value);

        const nowTime = getWATTimeStr();
        const start = settings['daylight_dispatch_start'] || '06:00';
        const end = settings['daylight_dispatch_end'] || '18:00';

        if (!isWithinWindow(nowTime, start, end)) {
            restrictedClasses = restrictedClasses.filter(c => c !== 'rider' && c !== 'agent' && c !== 'foot_agent');
            console.log(`[Dispatch] Night mode active. Restricting to Drivers only.`);
        }
    } catch (e) {
        console.warn('[Dispatch] Settings fetch failed for daylight window:', e.message);
    }

    const { rows } = await db.query(query, [order.pickup_location, radiusMeters, statePattern, restrictedClasses, rawState]);
    return rows;
  } catch (error) {
    console.error('[Dispatch] Search Error:', error.message);
    return [];
  }
};

/**
 * Broadcasts a mission offer to a list of fulfillers.
 * Includes automated radius expansion if no fulfillers found.
 */
const broadcastOffer = async (order, fulfillers = null) => {
  let targetFulfillers = fulfillers;

  if (!targetFulfillers || targetFulfillers.length === 0) {
      console.log(`[Dispatch] Broadening search to 50km for Order ${order.id}...`);
      targetFulfillers = await findNearbyFulfillers(order, 50000);
  }

  // Always broadcast to general online_fulfillers socket channel for instant UI popups
  try {
      const io = socketService.getIO();
      const payload = {
          order_id: order.id,
          pickup_address: order.pickup_address,
          delivery_address: order.delivery_address,
          total_fare: order.total_fare,
          item_description: order.item_description,
          created_at: new Date().toISOString()
      };
      io.to("online_fulfillers").emit("new_mission_offer", payload);
      console.log(`[Dispatch] Broadcasted offer for Order ${order.id} to room: online_fulfillers`);
  } catch (e) {
      console.warn(`[Dispatch] Broadcast to online_fulfillers failed:`, e.message);
  }

  if (!targetFulfillers || targetFulfillers.length === 0) {
      console.log(`[Dispatch] Zero direct targeted fulfillers found for Order ${order.id}.`);
      return;
  }

  const io = socketService.getIO();

  targetFulfillers.forEach(f => {
    console.log(`[Dispatch] Targeted notification to Fulfiller ${f.id} (User ${f.user_id}) for Mission ${order.id}`);

    // Push to Socket (Real-time App UI)
    io.to(`user_${f.user_id}`).emit("new_mission_offer", {
        order_id: order.id,
        pickup_address: order.pickup_address,
        delivery_address: order.delivery_address,
        total_fare: order.total_fare,
        item_description: order.item_description,
        distance_km: (f.dist / 1000).toFixed(1)
    });

    // PUSH Notification (Audible Ping)
    fcmService.sendNotification(
        f.user_id,
        "New Mission Nearby! 🚀",
        `Earn ₦${Math.ceil(order.total_fare * 0.80)} delivering: ${order.item_description}. Tap to view.`,
        {
            type: "MISSION_OFFER",
            order_id: order.id.toString(),
            sound: "default",
            priority: "high"
        }
    );
  });
};

module.exports = {
  findNearbyFulfillers,
  broadcastOffer
};
