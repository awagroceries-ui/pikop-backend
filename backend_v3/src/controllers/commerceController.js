const db = require('../config/db');
const crypto = require('crypto');
const axios = require('axios');
const PlatformConfig = require('../config/platform');
const { getWATTimeStr, isWithinWindow } = require('../utils/time');
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
               v.city, a.formatted_address as pickup_address, p.created_at, v.accepts_cod,
               v.operating_hours
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
               k.city, a.formatted_address as pickup_address, m.created_at, k.accepts_cod,
               k.operating_hours
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

    const nowTime = getWATTimeStr();

    // Mapping and manual filtering
    let mapped = rows.map(item => {
        let isOpen = true;
        let nextOpen = null;

        if (item.operating_hours) {
            const hours = typeof item.operating_hours === 'string' ? JSON.parse(item.operating_hours) : item.operating_hours;
            const today = new Date().getDay(); // 0-6 (Sun-Sat)
            const dayKey = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'][today];
            const config = hours[dayKey] || hours['all'];

            if (config) {
                isOpen = isWithinWindow(nowTime, config.open, config.close);
                nextOpen = config.open;
            }
        }

        return {
            ...item,
            is_open: isOpen,
            next_open_time: nextOpen,
            operating_hours: undefined // Hide from client payload
        };
    });

    let filtered = mapped;

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
    const { item_id, item_type, delivery_address, lat, lng, payment_method } = req.body;
    const userId = req.user.id;

    try {
        // 1. Fetch Item Details and Merchant Address
        let item;
        let merchantAddressId;
        if (item_type === 'product') {
            const res = await db.query(`
                SELECT p.*, v.pickup_address_id, v.business_name, v.user_id as merchant_user_id, v.operating_hours
                FROM products p JOIN vendors v ON v.id = p.vendor_id WHERE p.id = $1
            `, [item_id]);
            item = res.rows[0];
            merchantAddressId = item?.pickup_address_id;
        } else {
            const res = await db.query(`
                SELECT m.*, k.pickup_address_id, k.business_name, k.user_id as merchant_user_id, k.operating_hours
                FROM menu_items m JOIN kitchens k ON k.id = m.kitchen_id WHERE m.id = $1
            `, [item_id]);
            item = res.rows[0];
            merchantAddressId = item?.pickup_address_id;
        }

        if (!item) return res.status(404).json({ success: false, message: 'Item not found' });

        // 1.1 Enforcement of Operating Hours
        if (item.operating_hours) {
            const nowTime = getWATTimeStr();
            const hours = typeof item.operating_hours === 'string' ? JSON.parse(item.operating_hours) : item.operating_hours;
            const today = new Date().getDay();
            const dayKey = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'][today];
            const config = hours[dayKey] || hours['all'];

            if (config && !isWithinWindow(nowTime, config.open, config.close)) {
                return res.status(403).json({ success: false, message: `Store is currently closed. Opens at ${config.open}.` });
            }
        }

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

        // 4. Dual-Fee Concept Evaluation
        // Buyer-borne: Escrow Service Fee (Only applies if COD is chosen)
        let escrowRate = 0.10; // Default fallback
        try {
            const escrowRateRes = await db.query("SELECT value FROM settings WHERE key = 'cod_fee_rate'");
            if (escrowRateRes.rows.length > 0) escrowRate = parseFloat(escrowRateRes.rows[0].value);
        } catch (e) {}
        const platformFee = payment_method === 'COD' ? PlatformConfig.roundFee(item.price * escrowRate) : 0;

        // Seller-borne: Marketplace Commission (Applies always to reduce merchant payout)
        let commissionKey = 'shop_commission';
        let defaultRate = 0.10;
        if (item_type === 'meal' || (item.category && item.category.toLowerCase() === 'food')) {
            commissionKey = 'food_commission';
            defaultRate = 0.10;
        } else if (item.category && item.category.toLowerCase() === 'groceries') {
            commissionKey = 'groceries_commission';
            defaultRate = 0.05;
        }

        let commissionPercentage = defaultRate;
        try {
            const commissionRes = await db.query("SELECT value FROM settings WHERE key = $1", [commissionKey]);
            if (commissionRes.rows.length > 0) commissionPercentage = parseFloat(commissionRes.rows[0].value);
        } catch (e) {}
        const merchantCommissionAmount = PlatformConfig.roundFee(item.price * commissionPercentage);

        const totalNaira = parseFloat(item.price) + deliveryFee + platformFee;

        // --- COD FLOW (Bypass Paystack) ---
        if (payment_method === 'COD') {
            const client = await db.pool.connect();
            try {
                await client.query('BEGIN');

                // 4.1 Calculate Frozen Dispatch Commission (v3.9.8)
                let dispatchCommissionRate = 0.25;
                try {
                    const commRes = await client.query("SELECT value FROM settings WHERE key = 'platform_commission'");
                    if (commRes.rows.length > 0) dispatchCommissionRate = parseFloat(commRes.rows[0].value);
                } catch (e) {}
                const dispatchCommissionAmount = PlatformConfig.roundFee(deliveryFee * dispatchCommissionRate);

                // Insert directly into orders just like webhook does
                const orderRes = await client.query(
                    `INSERT INTO orders (
                        order_type, user_id, status, item_description,
                        pickup_address, delivery_address, pickup_location, delivery_location,
                        total_fare, item_price, delivery_fee, platform_fee_amount,
                        payment_status, collection_status, collect_on_delivery_amount,
                        payment_method, payment_channel,
                        seller_id, product_id, menu_item_id, escrow_status, merchant_commission_amount,
                        dispatch_commission_amount
                    ) VALUES (
                        'pickup_delivery', $1, 'SEARCHING', $2,
                        $3, $4, ST_SetSRID(ST_MakePoint($5, $6), 4326)::geography, ST_SetSRID(ST_MakePoint($7, $8), 4326)::geography,
                        $9, $10, $11, $12,
                        'PENDING', 'pending', $13,
                        'COD', 'cash',
                        $14, $15, $16, 'held', $17, $18
                    ) RETURNING id`,
                    [
                        userId, item.name,
                        item.pickup_address, delivery_address, mLoc.lng, mLoc.lat, lng, lat,
                        totalNaira, item.price, deliveryFee, platformFee,
                        totalNaira, // collect_on_delivery_amount
                        item.merchant_user_id, (item_type === 'product' ? item_id : null), (item_type === 'meal' ? item_id : null),
                        merchantCommissionAmount,
                        dispatchCommissionAmount
                    ]
                );

                const newOrderId = orderRes.rows[0].id;

                // Escrow hold (Pending since payment isn't collected yet, but we lock the ledger intent)
                const walletService = require('../services/walletService');
                const walletId = await walletService.ensureWalletExists(client, 'USER', item.merchant_user_id);
                await walletService.recordEntry(client, walletId, 'CREDIT', item.price, 'ESCROW_HOLD', `Marketplace COD Sale: ${item.name}`, newOrderId, 'pending');

                await client.query('COMMIT');

                // 4.3 Unified Guest Outreach (v3.9.9)
                const orderController = require('./orderController');
                await orderController.triggerInitialGuestCommunications(newOrderId);

                // Broadcast to fulfillers
                const dispatchService = require('../services/dispatchService');
                const updatedOrder = (await db.query("SELECT * FROM orders WHERE id = $1", [newOrderId])).rows[0];
                const fulfillers = await dispatchService.findNearbyFulfillers(updatedOrder);
                if (fulfillers.length > 0) dispatchService.broadcastOffer(updatedOrder, fulfillers).catch(() => {});

                return res.status(200).json({ success: true, order_id: newOrderId.toString() });
            } catch (err) {
                await client.query('ROLLBACK');
                throw err;
            } finally {
                client.release();
            }
        }

        // --- WALLET FLOW (Immediate Debit) ---
        if (payment_method === 'WALLETPAY') {
            const client = await db.pool.connect();
            try {
                await client.query('BEGIN');

                // 4.1 Calculate Frozen Dispatch Commission (v3.9.8)
                let dispatchCommissionRate = 0.25;
                try {
                    const commRes = await client.query("SELECT value FROM settings WHERE key = 'platform_commission'");
                    if (commRes.rows.length > 0) dispatchCommissionRate = parseFloat(commRes.rows[0].value);
                } catch (e) {}
                const dispatchCommissionAmount = PlatformConfig.roundFee(deliveryFee * dispatchCommissionRate);

                // Insert directly into orders
                const orderRes = await client.query(
                    `INSERT INTO orders (
                        order_type, user_id, status, item_description,
                        pickup_address, delivery_address, pickup_location, delivery_location,
                        total_fare, item_price, delivery_fee, platform_fee_amount,
                        payment_status, payment_method, payment_channel,
                        seller_id, product_id, menu_item_id, escrow_status, merchant_commission_amount,
                        dispatch_commission_amount
                    ) VALUES (
                        'pickup_delivery', $1, 'SEARCHING', $2,
                        $3, $4, ST_SetSRID(ST_MakePoint($5, $6), 4326)::geography, ST_SetSRID(ST_MakePoint($7, $8), 4326)::geography,
                        $9, $10, $11, $12,
                        'PAID', 'wallet', 'wallet',
                        $13, $14, $15, 'held', $16, $17
                    ) RETURNING id`,
                    [
                        userId, item.name,
                        item.pickup_address, delivery_address, mLoc.lng, mLoc.lat, lng, lat,
                        totalNaira, item.price, deliveryFee, platformFee,
                        item.merchant_user_id, (item_type === 'product' ? item_id : null), (item_type === 'meal' ? item_id : null),
                        merchantCommissionAmount,
                        dispatchCommissionAmount
                    ]
                );

                const newOrderId = orderRes.rows[0].id;

                // Individual Wallet Debit
                const walletService = require('../services/walletService');
                await walletService.processIndividualWalletPayment(client, userId, totalNaira, newOrderId);

                // Escrow hold for seller
                const walletId = await walletService.ensureWalletExists(client, 'USER', item.merchant_user_id);
                await walletService.recordEntry(client, walletId, 'CREDIT', item.price, 'ESCROW_HOLD', `Marketplace Sale: ${item.name}`, newOrderId, 'pending');

                await client.query('COMMIT');

                // Broadcast to fulfillers
                const dispatchService = require('../services/dispatchService');
                const updatedOrder = (await db.query("SELECT * FROM orders WHERE id = $1", [newOrderId])).rows[0];
                const fulfillers = await dispatchService.findNearbyFulfillers(updatedOrder);
                if (fulfillers.length > 0) dispatchService.broadcastOffer(updatedOrder, fulfillers).catch(() => {});

                return res.status(200).json({ success: true, order_id: newOrderId.toString() });
            } catch (err) {
                await client.query('ROLLBACK');
                throw err;
            } finally {
                client.release();
            }
        }

        // --- PREPAID FLOW (Paystack Initialization) ---
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
                merchant_commission_amount: merchantCommissionAmount,
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
