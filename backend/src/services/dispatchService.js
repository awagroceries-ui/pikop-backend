const db = require('../config/db');

/**
 * Finds nearby online fulfillers within a given radius, filtered by eligible classes.
 * @param {number} orderId - The ID of the order.
 * @param {number} radiusInKm - The search radius in kilometers.
 */
const findNearbyFulfillers = async (orderId, radiusInKm) => {
  const radiusInMeters = radiusInKm * 1000;

  const query = `
    SELECT f.id, f.user_id, f.mobility_type, ST_Distance(f.location, o.pickup_location) as distance
    FROM fulfillers f, orders o
    WHERE o.id = $1
    AND f.online_status = 'ONLINE'
    AND f.kyc_status = 'VERIFIED'
    AND (f.primary_class = ANY(o.eligible_classes) OR f.secondary_class = ANY(o.eligible_classes))
    AND ST_DWithin(f.location, o.pickup_location,
        CASE
          WHEN f.mobility_type = 'bicycle' THEN $2 * 1.5 -- 50% boost for cyclists
          ELSE $2
        END
    )
    ORDER BY distance ASC
  `;

  const { rows } = await db.query(query, [orderId, radiusInMeters]);
  return rows;
};

/**
 * Fetches available offers for a specific fulfiller based on their location and class.
 */
const findNearbyFulfillersForOffer = async (fulfillerId, baseRadiusKm) => {
    const baseRadiusMeters = baseRadiusKm * 1000;

    const query = `
      SELECT o.id, o.pickup_address, o.delivery_address, o.total_fare, o.created_at, o.item_photo_url, o.pickup_display_summary
      FROM orders o, fulfillers f
      WHERE f.id = $1
      AND o.status = 'SEARCHING'
      AND (f.primary_class = ANY(o.eligible_classes) OR f.secondary_class = ANY(o.eligible_classes))
      AND ST_DWithin(f.location, o.pickup_location,
        CASE
          WHEN f.mobility_type = 'bicycle' THEN $2 * 1.5
          ELSE $2
        END
      )
      ORDER BY o.created_at DESC
    `;

    const { rows } = await db.query(query, [fulfillerId, baseRadiusMeters]);

    return rows.map(r => ({
      id: r.id.toString(),
      pickup_address: r.pickup_display_summary || 'Restricted Area',
      delivery_address: 'Hidden until accepted',
      total_fare: parseFloat(r.total_fare),
      item_photo_url: r.item_photo_url,
      expires_at: new Date(new Date(r.created_at).getTime() + 5 * 60000).toISOString()
    }));
};

module.exports = {
  findNearbyFulfillers,
  findNearbyFulfillersForOffer
};
