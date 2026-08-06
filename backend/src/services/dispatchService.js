const db = require('../config/db');

/**
 * Finds nearby online fulfillers within a given radius.
 * @param {number} orderId - The ID of the order.
 * @param {number} radiusInKm - The search radius in kilometers.
 */
const findNearbyFulfillers = async (orderId, radiusInKm) => {
  const radiusInMeters = radiusInKm * 1000;

  const query = `
    SELECT f.id, f.user_id, ST_Distance(f.location, o.pickup_location) as distance
    FROM fulfillers f, orders o
    WHERE o.id = $1
    AND f.online_status = 'ONLINE'
    AND f.kyc_status = 'VERIFIED'
    AND ST_DWithin(f.location, o.pickup_location, $2)
    ORDER BY distance ASC
  `;

  const { rows } = await db.query(query, [orderId, radiusInMeters]);
  return rows;
};

module.exports = {
  findNearbyFulfillers
};
