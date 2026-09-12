const db = require('../config/db');
const crypto = require('crypto');
const axios = require('axios');
const PlatformConfig = require('../config/platform');
const PAYSTACK_SECRET = (process.env.PAYSTACK_SECRET_KEY || '').trim();

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

/**
 * Initializes a Commerce Order (Buy + Deliver).
 */
const initializeCommerceOrder = async (req, res) => {
    const { item_id, item_type, delivery_address, lat, lng } = req.body;
    const userId = req.user.id;

    try {
        // 1. Fetch Item Details and Merchant Address
        let item;
        let merchantAddressId;
        if (item_type === 'product') {
            const res = await db.query(`
                SELECT p.*, v.pickup_address_id, v.business_name, v.user_id as merchant_user_id
                FROM products p JOIN vendors v ON v.id = p.vendor_id WHERE p.id = $1
            `, [item_id]);
            item = res.rows[0];
            merchantAddressId = item?.pickup_address_id;
        } else {
            const res = await db.query(`
                SELECT m.*, k.pickup_address_id, k.business_name, k.user_id as merchant_user_id
                FROM menu_items m JOIN kitchens k ON k.id = m.kitchen_id WHERE m.id = $1
            `, [item_id]);
            item = res.rows[0];
            merchantAddressId = item?.pickup_address_id;
        }

        if (!item) return res.status(404).json({ success: false, message: 'Item not found' });

        // 2. Fetch Merchant Coordinates
        const addrRes = await db.query("SELECT ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng FROM addresses WHERE id = $1", [merchantAddressId]);
        const mLoc = addrRes.rows[0];

        // 3. Calculate Distance-based Delivery Fee
        const distRes = await db.query(
            "SELECT ST_Distance(ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography, ST_SetSRID(ST_MakePoint($3, $4), 4326)::geography) / 1000 as dist",
            [mLoc.lng, mLoc.lat, lng, lat]
        );
        const distanceKm = parseFloat(distRes.rows[0].dist || 5.0);

        const baseFee = item_type === 'meal' ? 500 : 800; // Simplified logic
        const deliveryFee = Math.ceil(baseFee + (distanceKm * 110));
        const platformFee = PlatformConfig.roundFee(item.price * 0.10); // 10% Escrow Fee

        const totalNaira = parseFloat(item.price) + deliveryFee + platformFee;

        // 4. Initialize Paystack
        const payload = {
            amount: Math.round(totalNaira * 100),
            email: req.user.email,
            callback_url: 'https://api.pikop.com.ng/api/v1/payments/webhook',
            metadata: {
                type: 'COMMERCE_ORDER',
                user_id: userId,
                item_id,
                item_type,
                item_price: parseFloat(item.price),
                delivery_fee: deliveryFee,
                platform_fee_amount: platformFee,
                vendor_id: item.vendor_id || item.kitchen_id,
                merchant_user_id: item.merchant_user_id,
                pickup_address_id: merchantAddressId,
                delivery_address,
                delivery_lat: lat,
                delivery_lng: lng,
                item_description: item.name
            }
        };

        const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
        });

        res.status(200).json(response.data.data);

    } catch (error) {
        console.error('[Commerce] Init Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

module.exports = {
  getDiscovery,
  initializeCommerceOrder
};
