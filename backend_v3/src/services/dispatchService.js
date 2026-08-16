const db = require('../config/db');
const socketService = require('./socketService');
const fcmService = require('./fcmService');

/**
 * Shared Dispatch Engine - v3 (Milestone 6)
 */
const findNearbyFulfillers = async (order) => {
  const radiusMeters = 10000; // 10km base radius

  try {
    // V3 Advanced Dispatch (Milestone 6 + Prompt 5 Capacity)
    // Filter:
    // 1. Must be ONLINE and VERIFIED
    // 2. Must be within Radius
    // 3. Must have remaining Queue Capacity based on class
    const query = `
      SELECT f.user_id, f.id, f.primary_class, ST_Distance(f.current_location, $1) as dist,
             (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) as load
      FROM fulfillers f
      WHERE f.online_status = 'ONLINE'
      AND f.kyc_status = 'VERIFIED'
      AND ST_DWithin(f.current_location, $1, $2)
      AND (
          (f.primary_class = 'agent' AND (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) < 2)
          OR (f.primary_class = 'rider' AND (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) < 5)
          OR (f.primary_class = 'driver' AND (SELECT COUNT(*) FROM orders WHERE (fulfiller_id = f.id OR queued_for_fulfiller_id = f.id) AND status NOT IN ('DELIVERED', 'CANCELLED')) < 15)
      )
      ORDER BY dist ASC
      LIMIT 20
    `;

    const { rows } = await db.query(query, [order.pickup_location, radiusMeters]);
    return rows;
  } catch (error) {
    console.error('[Dispatch] Search Error:', error.message);
    return [];
  }
};

/**
 * Broadcasts a mission offer to a list of fulfillers.
 */
const broadcastOffer = async (order, fulfillers) => {
  const io = socketService.getIO();

  fulfillers.forEach(f => {
    console.log(`[Dispatch] Notifying Fulfiller ${f.id} of Mission ${order.id}`);

    // Push to Socket (Real-time App UI)
    io.to(`user_${f.user_id}`).emit("new_mission_offer", {
        order_id: order.id,
        pickup_address: order.pickup_address,
        total_fare: order.total_fare,
        item_description: order.item_description,
        distance_km: (f.dist / 1000).toFixed(1)
    });

    // PUSH Notification (v3.5.1)
    fcmService.sendNotification(f.user_id, "New Mission Offer", `Earn ₦${Math.ceil(order.total_fare * 0.75)} with a new delivery nearby.`);
  });
};

module.exports = {
  findNearbyFulfillers,
  broadcastOffer
};
