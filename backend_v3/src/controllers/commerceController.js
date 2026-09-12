const db = require('../config/db');

/**
 * Fetches unified discovery items (Products and Menu Items).
 * Supports proximity sorting, search, and category filtering.
 */
const getDiscovery = async (req, res) => {
  const { lat, lng, category, query, limit = 50 } = req.query;

  try {
    const params = [];
    let locationSelect = "";
    let distanceJoin = "";
    let orderBy = "created_at DESC";

    if (lat && lng) {
        locationSelect = `, ST_Distance(a.location::geography, ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography) / 1000 as distance_km`;
        params.push(lng, lat);
        orderBy = "distance_km ASC";
    }

    // Unified Query: General Products + Kitchen Menu Items
    const sql = `
      (
        SELECT p.id, p.name, p.price, p.photo_url, p.category, p.description,
               v.business_name as vendor_name, v.id::text as vendor_id, 'product' as item_type,
               v.city, a.formatted_address as pickup_address, p.created_at
               ${locationSelect}
        FROM products p
        JOIN vendors v ON v.id = p.vendor_id
        LEFT JOIN addresses a ON a.id = v.pickup_address_id
        WHERE p.active = true AND v.status = 'active'
      )
      UNION ALL
      (
        SELECT m.id::text, m.name, m.price, m.photo_url, m.category, m.description,
               k.business_name as vendor_name, k.id::text as vendor_id, 'meal' as item_type,
               k.city, a.formatted_address as pickup_address, m.created_at
               ${locationSelect}
        FROM menu_items m
        JOIN kitchens k ON k.id = m.kitchen_id
        LEFT JOIN addresses a ON a.id = k.pickup_address_id
        WHERE m.available = true AND k.status = 'active'
      )
      ORDER BY ${orderBy}
      LIMIT $${params.length + 1}
    `;

    params.push(limit);

    const { rows } = await db.query(sql, params);

    // Manual filtering for Category and Query (SQL UNION makes complex WHERE tricky, easier to filter or use a wrapper)
    let filtered = rows;

    if (category && category.toLowerCase() !== 'all') {
        filtered = filtered.filter(item =>
            item.category.toLowerCase() === category.toLowerCase() ||
            (category.toLowerCase() === 'food' && item.item_type === 'meal')
        );
    }

    if (query) {
        const q = query.toLowerCase();
        filtered = filtered.filter(item =>
            item.name.toLowerCase().includes(q) ||
            item.vendor_name.toLowerCase().includes(q) ||
            (item.description && item.description.toLowerCase().includes(q))
        );
    }

    res.status(200).json({
      success: true,
      data: filtered
    });

  } catch (error) {
    console.error('[Commerce] Discovery Error:', error.message);
    res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  getDiscovery
};
