const db = require('../config/db');
const socketService = require('./socketService');

/**
 * Shared Dispatch Engine - v3 (Milestone 6)
 */
const findNearbyFulfillers = async (order) => {
  const radiusMeters = 10000; // 10km base radius

  try {
    // 1. Geography-based proximity search
    // Filter: Must be ONLINE, VERIFIED, and match Size-tier eligibility
    const query = `
      SELECT f.user_id, f.id, ST_Distance(f.current_location, $1) as dist
      FROM fulfillers f
      WHERE f.online_status = 'ONLINE'
      AND f.kyc_status = 'VERIFIED'
      AND ST_DWithin(f.current_location, $1, $2)
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
  });
};

module.exports = {
  findNearbyFulfillers,
  broadcastOffer
};
