const db = require('../config/db');
const crypto = require('crypto');

/**
 * Sets up a new Merchant Profile (Stage 2 of Onboarding).
 */
const setupMerchantProfile = async (req, res) => {
    const userId = req.user.id;
    const { business_name, category, address, cac_number, nafdac_number, bank_name, account_number, accepts_cod = true } = req.body;

    // Slug generation (v4.7)
    const store_slug = business_name.toLowerCase().trim().replace(/[^a-z0-9]/g, '-').replace(/-+/g, '-');

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        let profileId;
        const initialStatus = 'pending_business_verification';

        // Check if profile already exists
        const [vendorRes, kitchenRes] = await Promise.all([
            client.query("SELECT id FROM vendors WHERE user_id = $1", [userId]),
            client.query("SELECT id FROM kitchens WHERE user_id = $1", [userId])
        ]);

        if (vendorRes.rows.length > 0 || kitchenRes.rows.length > 0) {
            await client.query('ROLLBACK');
            return res.status(400).json({ success: false, message: 'Merchant profile already exists.' });
        }

        // We assume category dictates table logic for simplicity
        if (category === 'Food') {
            const result = await client.query(
                `INSERT INTO kitchens (user_id, business_name, status, accepts_cod, category, store_slug)
                 VALUES ($1, $2, $3, $4, $5, $6) RETURNING id`,
                [userId, business_name, initialStatus, accepts_cod, category, store_slug]
            );
            profileId = result.rows[0].id;
        } else {
            const result = await client.query(
                `INSERT INTO vendors (user_id, business_name, status, accepts_cod, category, store_slug)
                 VALUES ($1, $2, $3, $4, $5, $6) RETURNING id`,
                [userId, business_name, initialStatus, accepts_cod, category, store_slug]
            );
            profileId = result.rows[0].id;
        }

        // Store docs manually in kyc_documents if provided
        if (cac_number) {
            await client.query(
                `INSERT INTO kyc_documents (user_id, doc_type, file_url, status)
                 VALUES ($1, 'CAC', $2, 'PENDING')`,
                [userId, cac_number]
            );
        }

        if (nafdac_number) {
            await client.query(
                `INSERT INTO kyc_documents (user_id, doc_type, file_url, status)
                 VALUES ($1, 'NAFDAC', $2, 'PENDING')`,
                [userId, nafdac_number]
            );
        }

        await client.query('COMMIT');
        res.status(201).json({ success: true, message: 'Business setup submitted successfully.' });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error(error);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

/**
 * Registers a new Merchant Account and generates an API key.
 */
const registerMerchant = async (req, res) => {
  const { business_name, contact_email } = req.body;
  const userId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Generate API Key
    const apiKey = `pk_live_${crypto.randomBytes(24).toString('hex')}`;
    const hash = crypto.createHash('sha256').update(apiKey).digest('hex');

    // 2. Create Merchant Account
    const merchantRes = await client.query(
      "INSERT INTO merchant_accounts (business_name, contact_email, api_key_hash) VALUES ($1, $2, $3) RETURNING id",
      [business_name, contact_email, hash]
    );
    const merchantId = merchantRes.rows[0].id;

    // 3. Link Owner
    await client.query(
      "INSERT INTO merchant_sub_accounts (merchant_account_id, user_id, role) VALUES ($1, $2, 'admin')",
      [merchantId, userId]
    );

    await client.query('COMMIT');

    res.status(201).json({
      success: true,
      message: 'Merchant account registered successfully.',
      data: {
        merchant_id: merchantId,
        api_key: apiKey // ONLY SHOWN ONCE
      }
    });
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
};

/**
 * Handles bulk mission creation (Milestone 19).
 */
const createBulkOrders = async (req, res) => {
  const { orders } = req.body; // Array of order objects
  const merchantId = req.merchant.id;

  if (!Array.isArray(orders) || orders.length === 0) {
    return res.status(400).json({ success: false, message: 'Invalid payload: orders array required' });
  }

  const batchId = crypto.randomUUID();

  try {
    // 1. Create Batch Record
    await db.query(
      "INSERT INTO order_batches (id, merchant_account_id, name, total_orders) VALUES ($1, $2, $3, $4)",
      [batchId, merchantId, `Bulk_${new Date().toISOString()}`, orders.length]
    );

    // 2. Queue orders for processing (Alpha: process immediately)
    // In a real production V3, this would push to a Redis BullMQ worker.
    for (const order of orders) {
      try {
        await db.query(
          `INSERT INTO orders (
            order_type, user_id, merchant_account_id, batch_id, status,
            pickup_address, delivery_address,
            pickup_location, delivery_location,
            total_fare, item_description, payment_status
          ) VALUES (
            'pickup_delivery', $1, $2, $3, 'SEARCHING',
            $4, $5,
            ST_SetSRID(ST_MakePoint($6, $7), 4326), ST_SetSRID(ST_MakePoint($8, $9), 4326),
            $10, $11, 'PAID'
          )`,
          [
            req.user?.id || null, merchantId, batchId,
            order.pickup_address, order.delivery_address,
            order.pickup_lng, order.pickup_lat, order.delivery_lng, order.delivery_lat,
            order.total_fare || 1500, // Pre-calculated or flat for alpha bulk
            order.item_description || 'Bulk Item'
          ]
        );
      } catch (e) {
        console.error(`[Bulk] Failed to insert row: ${e.message}`);
      }
    }

    // 3. Complete Batch
    await db.query("UPDATE order_batches SET status = 'completed', processed_orders = total_orders WHERE id = $1", [batchId]);

    res.status(201).json({
      success: true,
      message: 'Bulk missions created and broadcasted.',
      data: { batch_id: batchId, count: orders.length }
    });

  } catch (error) {
    throw error;
  }
};

/**
 * Returns batches for a merchant.
 */
const getBatches = async (req, res) => {
    const merchantId = req.merchant.id;
    try {
        const { rows } = await db.query(
            "SELECT * FROM order_batches WHERE merchant_account_id = $1 ORDER BY created_at DESC",
            [merchantId]
        );
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        throw error;
    }
};

/**
 * Returns detailed status of a bulk batch.
 */
const getBatchStatus = async (req, res) => {
    const { batchId } = req.params;
    const merchantId = req.merchant.id;

    try {
        const { rows } = await db.query(
            "SELECT * FROM order_batches WHERE id = $1 AND merchant_account_id = $2",
            [batchId, merchantId]
        );

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Batch not found' });

        const batch = rows[0];

        // Fetch order summaries in this batch
        const orders = await db.query(
            "SELECT id, status, total_fare FROM orders WHERE batch_id = $1",
            [batchId]
        );

        res.status(200).json({
            success: true,
            data: {
                ...batch,
                orders: orders.rows
            }
        });
    } catch (error) {
        throw error;
    }
};

/**
 * Returns batches owned by the authenticated user.
 */
const getMyBatches = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(`
            SELECT b.*
            FROM order_batches b
            JOIN merchant_accounts ma ON ma.id = b.merchant_account_id
            JOIN merchant_sub_accounts msa ON msa.merchant_account_id = ma.id
            WHERE msa.user_id = $1
            ORDER BY b.created_at DESC
        `, [userId]);
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        throw error;
    }
};

/**
 * Returns a unified dashboard for the seller/merchant.
 */
const getSellerDashboard = async (req, res) => {
    const userId = req.user.id;
    try {
        // 1. Fetch Sales (Missions where user is the seller)
        const { rows: sales } = await db.query(`
            SELECT o.*,
            ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
            ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng
            FROM orders o
            WHERE o.seller_id = $1
            ORDER BY o.created_at DESC
            LIMIT 50
        `, [userId]);

        // 2. Fetch Marketplace Products OR Kitchen Menu Items
        const [vProfile, kProfile] = await Promise.all([
            db.query("SELECT id FROM vendors WHERE user_id = $1", [userId]),
            db.query("SELECT id FROM kitchens WHERE user_id = $1", [userId])
        ]);

        let products = [];
        if (vProfile.rows.length > 0) {
            const res = await db.query(`
                SELECT p.*, 'vendor' as merchant_type
                FROM products p
                WHERE p.vendor_id = $1
                ORDER BY p.created_at DESC
            `, [vProfile.rows[0].id]);
            products = res.rows;
        } else if (kProfile.rows.length > 0) {
            const res = await db.query(`
                SELECT m.*, 'kitchen' as merchant_type, m.available as active
                FROM menu_items m
                WHERE m.kitchen_id = $1
                ORDER BY m.created_at DESC
            `, [kProfile.rows[0].id]);
            products = res.rows; // Map menu_items to product-like structure for the dashboard
        }

        // 3. Fetch Bulk Batches
        const { rows: batches } = await db.query(`
            SELECT b.*
            FROM order_batches b
            JOIN merchant_accounts ma ON ma.id = b.merchant_account_id
            JOIN merchant_sub_accounts msa ON msa.merchant_account_id = ma.id
            WHERE msa.user_id = $1
            ORDER BY b.created_at DESC
        `, [userId]);

        if (sales.length === 0 && products.length === 0 && batches.length === 0) {
            console.log(`[Merchant] Empty Dashboard for user ${userId}. Checking if active profiles exist.`);
            const [v, k] = await Promise.all([
                db.query("SELECT id FROM vendors WHERE user_id = $1", [userId]),
                db.query("SELECT id FROM kitchens WHERE user_id = $1", [userId])
            ]);
            if (v.rows.length === 0 && k.rows.length === 0) {
                console.warn(`[Merchant] User ${userId} requested dashboard but has NO business profile.`);
            }
        }

        res.status(200).json({
            success: true,
            data: {
                sales,
                products,
                batches
            }
        });
    } catch (error) {
        console.error('[Merchant] Dashboard Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns the user's active merchant profile (Vendor or Kitchen).
 */
const getMerchantProfile = async (req, res) => {
    const userId = req.user.id;

    try {
        const [vendorRes, kitchenRes] = await Promise.all([
            db.query("SELECT *, 'vendor' as type FROM vendors WHERE user_id = $1", [userId]),
            db.query("SELECT *, 'kitchen' as type FROM kitchens WHERE user_id = $1", [userId])
        ]);

        const profile = vendorRes.rows[0] || kitchenRes.rows[0] || null;

        res.status(200).json({
            success: true,
            data: profile
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Handles bulk mission creation for an authenticated user session (v3.9.9).
 */
const createBulkOrdersSession = async (req, res) => {
    const { orders, batch_name } = req.body; // Array of order objects
    const userId = req.user.id;

    if (!Array.isArray(orders) || orders.length === 0) {
        return res.status(400).json({ success: false, message: 'Invalid payload: orders array required' });
    }

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Verify User owns a Merchant Account
        const merchantRes = await client.query(`
            SELECT ma.id FROM merchant_accounts ma
            JOIN merchant_sub_accounts msa ON msa.merchant_account_id = ma.id
            WHERE msa.user_id = $1 AND msa.role = 'admin'
            LIMIT 1
        `, [userId]);

        if (merchantRes.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(403).json({ success: false, message: 'Merchant account required for bulk dispatch' });
        }
        const merchantId = merchantRes.rows[0].id;

        // 2. Calculate Total Batch Cost
        let totalBatchCost = 0;
        orders.forEach(o => { totalBatchCost += parseFloat(o.total_fare || 1500); });

        // 3. Verify and Debit Wallet
        const walletId = await walletService.ensureWalletExists(client, 'USER', userId);
        const { rows: wallet } = await client.query("SELECT balance FROM wallets WHERE id = $1 FOR UPDATE", [walletId]);

        if (parseFloat(wallet[0].balance) < totalBatchCost) {
            await client.query('ROLLBACK');
            return res.status(400).json({ success: false, message: `Insufficient balance. Total required: ₦${totalBatchCost.toLocaleString()}. Current balance: ₦${parseFloat(wallet[0].balance).toLocaleString()}` });
        }

        await walletService.recordEntry(client, walletId, 'DEBIT', totalBatchCost, 'BULK_DISPATCH', `Payment for batch: ${batch_name || 'In-App Bulk'}`);

        // 4. Create Batch Record
        const batchId = crypto.randomUUID();
        await client.query(
            "INSERT INTO order_batches (id, merchant_account_id, name, total_orders, status) VALUES ($1, $2, $3, $4, 'processing')",
            [batchId, merchantId, batch_name || `App_Bulk_${new Date().toISOString()}`, orders.length]
        );

        // 5. Insert Orders
        for (const order of orders) {
            const pCode = Math.floor(1000 + Math.random() * 9000).toString();
            const dCode = Math.floor(1000 + Math.random() * 9000).toString();
            const pHash = await bcrypt.hash(pCode, 10);
            const dHash = await bcrypt.hash(dCode, 10);

            await client.query(
                `INSERT INTO orders (
                    order_type, user_id, merchant_account_id, batch_id, status,
                    pickup_address, delivery_address,
                    pickup_location, delivery_location,
                    total_fare, item_description, payment_status,
                    pickup_code, delivery_code, pickup_code_hash, delivery_code_hash
                ) VALUES (
                    'pickup_delivery', $1, $2, $3, 'SEARCHING',
                    $4, $5,
                    ST_SetSRID(ST_MakePoint($6, $7), 4326), ST_SetSRID(ST_MakePoint($8, $9), 4326),
                    $10, $11, 'PAID',
                    $12, $13, $14, $15
                )`,
                [
                    userId, merchantId, batchId,
                    order.pickup_address, order.delivery_address,
                    order.pickup_lng || 0, order.pickup_lat || 0, order.delivery_lng || 0, order.delivery_lat || 0,
                    order.total_fare || 1500,
                    order.item_description || 'Bulk Dispatch Item',
                    pCode, dCode, pHash, dHash
                ]
            );
        }

        // 6. Finalize Batch
        await client.query("UPDATE order_batches SET status = 'completed', processed_orders = total_orders WHERE id = $1", [batchId]);

        await client.query('COMMIT');

        res.status(201).json({
            success: true,
            message: 'Bulk batch activated successfully.',
            data: { batch_id: batchId, count: orders.length, total_cost: totalBatchCost }
        });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Merchant] Bulk Session Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

/**
 * Updates merchant settings (e.g. COD preference, Operating Hours).
 */
const updateMerchantSettings = async (req, res) => {
    const userId = req.user.id;
    const { accepts_cod, operating_hours } = req.body;

    try {
        const queries = [
            db.query("UPDATE vendors SET accepts_cod = COALESCE($1, accepts_cod), operating_hours = COALESCE($2, operating_hours) WHERE user_id = $3", [accepts_cod, operating_hours ? JSON.stringify(operating_hours) : null, userId]),
            db.query("UPDATE kitchens SET accepts_cod = COALESCE($1, accepts_cod), operating_hours = COALESCE($2, operating_hours) WHERE user_id = $3", [accepts_cod, operating_hours ? JSON.stringify(operating_hours) : null, userId])
        ];
        await Promise.all(queries);
        res.status(200).json({ success: true, message: 'Settings updated successfully.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Fetches incoming orders for the merchant.
 */
const getIncomingOrders = async (req, res) => {
    const userId = req.user.id;
    try {
        // Fetch the merchant account
        const mRes = await db.query(
            `SELECT id FROM vendors WHERE user_id = $1
             UNION
             SELECT id FROM kitchens WHERE user_id = $1`,
            [userId]
        );

        if (mRes.rows.length === 0) return res.status(404).json({ success: false, message: 'Merchant not found' });
        const merchantId = mRes.rows[0].id;

        // Fetch orders linked to this merchant (via product or menu_item)
        const { rows } = await db.query(
            `SELECT o.*
             FROM orders o
             WHERE o.merchant_account_id = $1
             ORDER BY o.created_at DESC`,
            [merchantId]
        );

        res.status(200).json(rows);
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Updates order status by the merchant.
 */
const updateOrderStatus = async (req, res) => {
    const { id } = req.params;
    const { status } = req.body;
    const userId = req.user.id;

    try {
        // Validate ownership
        const oRes = await db.query(
            `SELECT o.id, o.merchant_account_id
             FROM orders o
             JOIN vendors v ON v.id = o.merchant_account_id AND v.user_id = $2
             WHERE o.id = $1
             UNION
             SELECT o.id, o.merchant_account_id
             FROM orders o
             JOIN kitchens k ON k.id = o.merchant_account_id AND k.user_id = $2
             WHERE o.id = $1`,
            [id, userId]
        );

        if (oRes.rows.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });

        await db.query("UPDATE orders SET status = $1 WHERE id = $2", [status, id]);

        // Broadcast socket alert
        try {
            const socketService = require('../services/socketService');
            socketService.getIO().emit(`order_update_${id}`, { status });
        } catch (e) {}

        res.status(200).json({ success: true, message: 'Order status updated' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns pending and active return requests for the merchant.
 */
const getReturnRequests = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(`
            SELECT r.*, o.item_description, o.item_price, u.full_name as customer_name
            FROM returns r
            JOIN orders o ON o.id = r.order_id
            JOIN users u ON u.id = o.user_id
            WHERE (o.vendor_id IN (SELECT id FROM vendors WHERE user_id = $1)
               OR o.kitchen_id IN (SELECT id FROM kitchens WHERE user_id = $1))
            ORDER BY r.created_at DESC
        `, [userId]);
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Processes a return request (Approve/Decline).
 */
const processReturnRequest = async (req, res) => {
    const { returnId } = req.params;
    const { status, merchant_notes, delivery_fee_payer } = req.body;
    const userId = req.user.id;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Fetch Return & Verify Ownership
        const { rows } = await client.query(`
            SELECT r.*, o.user_id as customer_user_id, o.pickup_location as original_pickup, o.delivery_location as original_delivery,
                   o.delivery_address as customer_address, o.pickup_address as merchant_address,
                   o.item_description, o.total_fare as original_fare
            FROM returns r
            JOIN orders o ON o.id = r.order_id
            WHERE r.id = $1 FOR UPDATE
        `, [returnId]);

        if (rows.length === 0) throw new Error('Return request not found');
        const ret = rows[0];

        // 2. Update Status
        await client.query(
            "UPDATE returns SET status = $1, merchant_notes = $2, delivery_fee_payer = $3 WHERE id = $4",
            [status, merchant_notes, delivery_fee_payer || 'CUSTOMER', returnId]
        );

        // 3. If Approved, Generate Reverse Mission
        if (status === 'APPROVED') {
            const initialStatus = delivery_fee_payer === 'MERCHANT' ? 'SEARCHING' : 'AWAITING_PAYMENT';

            // Note: Reuse original total_fare for return trip as a baseline
            const returnFare = ret.original_fare;

            const orderRes = await client.query(
                `INSERT INTO orders (
                    order_type, user_id, status, item_description,
                    pickup_address, delivery_address, pickup_location, delivery_location,
                    total_fare, payment_status, initiator_role, parent_order_id
                ) VALUES (
                    'pickup_delivery', $1, $2, $3,
                    $4, $5, $6, $7,
                    $8, $9, 'SELLER', $10
                ) RETURNING id`,
                [
                    ret.customer_user_id, initialStatus, `RETURN: ${ret.item_description}`,
                    ret.customer_address, ret.merchant_address, ret.original_delivery, ret.original_pickup,
                    returnFare, initialStatus === 'SEARCHING' ? 'PAID' : 'pending', ret.order_id
                ]
            );

            const newOrderId = orderRes.rows[0].id;
            await client.query("UPDATE returns SET return_delivery_order_id = $1 WHERE id = $2", [newOrderId, returnId]);

            // Handle Merchant Payout if they cover delivery
            if (delivery_fee_payer === 'MERCHANT') {
                const walletService = require('../services/walletService');
                const walletId = await walletService.ensureWalletExists(client, 'USER', userId);
                await walletService.recordEntry(client, walletId, 'DEBIT', returnFare, 'SETTLEMENT', `Payment for return delivery #${newOrderId}`, newOrderId);
            }
        }

        await client.query('COMMIT');
        res.status(200).json({ success: true, message: `Return request ${status.toLowerCase()}.` });

    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

/**
 * Confirms receipt of returned item and triggers refund.
 */
const confirmReturnReceipt = async (req, res) => {
    const { returnId } = req.params;
    const userId = req.user.id;

    try {
        await db.query("UPDATE returns SET status = 'RECEIVED' WHERE id = $1", [returnId]);

        // Trigger Wallet Refund (Milestone 4.2)
        const walletService = require('../services/walletService');
        await walletService.processReturnRefund(returnId);

        res.status(200).json({ success: true, message: 'Return received. Refund processed to customer.' });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Aggregates business performance metrics for the merchant (v4.5).
 */
const getMerchantAnalytics = async (req, res) => {
    const userId = req.user.id;
    const { range = 'weekly' } = req.query;

    try {
        // 1. Time Boundaries (WAT)
        let interval, truncate, subPeriod;
        switch (range) {
            case 'daily': interval = '1 day'; truncate = 'day'; subPeriod = 'hour'; break;
            case 'monthly': interval = '1 month'; truncate = 'month'; subPeriod = 'day'; break;
            case 'annual': interval = '1 year'; truncate = 'year'; subPeriod = 'month'; break;
            default: interval = '1 week'; truncate = 'week'; subPeriod = 'day'; break;
        }

        const start = `DATE_TRUNC('${truncate}', NOW() AT TIME ZONE 'Africa/Lagos')`;
        const end = `(${start} + INTERVAL '${interval}')`;

        // 2. Volume & Revenue Trend (Net Revenue)
        const trendRes = await db.query(`
            SELECT
                DATE_TRUNC($1, created_at AT TIME ZONE 'Africa/Lagos') as period,
                COUNT(*) as volume,
                COALESCE(SUM(item_price - merchant_commission_amount), 0) as net_revenue
            FROM orders
            WHERE seller_id = $2 AND status IN ('DELIVERED', 'RELEASED')
            AND created_at >= ${start} AND created_at < ${end}
            GROUP BY 1 ORDER BY period ASC
        `, [subPeriod, userId]);

        // 3. Best Selling Items
        const itemsRes = await db.query(`
            SELECT
                COALESCE(p.name, m.name, o.item_description) as name,
                COUNT(*) as units,
                COALESCE(SUM(o.item_price - o.merchant_commission_amount), 0) as revenue
            FROM orders o
            LEFT JOIN products p ON p.id = o.product_id
            LEFT JOIN menu_items m ON m.id = o.menu_item_id
            WHERE o.seller_id = $1 AND o.status IN ('DELIVERED', 'RELEASED')
            AND o.created_at >= ${start} AND o.created_at < ${end}
            GROUP BY 1 ORDER BY units DESC LIMIT 5
        `, [userId]);

        // 4. Repeat Customer Rate (Lifetime Context)
        const retentionRes = await db.query(`
            WITH customer_orders AS (
                SELECT user_id, COUNT(*) as order_count
                FROM orders
                WHERE seller_id = $1 AND status IN ('DELIVERED', 'RELEASED')
                GROUP BY user_id
            )
            SELECT
                COUNT(*) as total_customers,
                COUNT(*) FILTER (WHERE order_count > 1) as repeat_customers
            FROM customer_orders
        `, [userId]);

        const r = retentionRes.rows[0];
        const totalCustomers = parseInt(r.total_customers || 0);
        const repeatRate = totalCustomers > 0
            ? (parseInt(r.repeat_customers) / totalCustomers * 100).toFixed(1)
            : 0;

        // 5. Peak Ordering Times
        const peaksRes = await db.query(`
            SELECT
                EXTRACT(DOW FROM created_at AT TIME ZONE 'Africa/Lagos') as day,
                EXTRACT(HOUR FROM created_at AT TIME ZONE 'Africa/Lagos') as hour,
                COUNT(*) as volume
            FROM orders
            WHERE seller_id = $1 AND status IN ('DELIVERED', 'RELEASED')
            AND created_at >= ${start} AND created_at < ${end}
            GROUP BY 1, 2 ORDER BY volume DESC LIMIT 5
        `, [userId]);

        res.status(200).json({
            success: true,
            data: {
                range,
                trend: trendRes.rows,
                best_sellers: itemsRes.rows,
                retention: {
                    total_unique_customers: totalCustomers,
                    repeat_customer_rate: parseFloat(repeatRate)
                },
                peak_times: peaksRes.rows
            }
        });

    } catch (error) {
        console.error('[MerchantAnalytics] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Resolves merchant details by their store slug (v4.7).
 */
const getMerchantBySlug = async (req, res) => {
    const { slug } = req.params;
    try {
        const [vendor, kitchen] = await Promise.all([
            db.query("SELECT id, 'vendor' as type FROM vendors WHERE store_slug = $1", [slug]),
            db.query("SELECT id, 'kitchen' as type FROM kitchens WHERE store_slug = $1", [slug])
        ]);

        const match = vendor.rows[0] || kitchen.rows[0];
        if (!match) return res.status(404).json({ success: false, message: 'Store not found' });

        res.status(200).json({ success: true, data: match });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns coupons for the authenticated merchant (v4.7).
 */
const getMerchantCoupons = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows } = await db.query(`
            SELECT c.* FROM coupons c
            WHERE c.merchant_id IN (SELECT id FROM vendors WHERE user_id = $1)
               OR c.kitchen_id IN (SELECT id FROM kitchens WHERE user_id = $1)
            ORDER BY c.created_at DESC
        `, [userId]);
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Creates a merchant-specific coupon (v4.7).
 */
const createMerchantCoupon = async (req, res) => {
    const userId = req.user.id;
    const { code, discount_type, discount_value, min_order_amount, usage_limit } = req.body;

    try {
        const [vendorRes, kitchenRes] = await Promise.all([
            db.query("SELECT id FROM vendors WHERE user_id = $1", [userId]),
            db.query("SELECT id FROM kitchens WHERE user_id = $1", [userId])
        ]);

        const vendorId = vendorRes.rows[0]?.id;
        const kitchenId = kitchenRes.rows[0]?.id;

        if (!vendorId && !kitchenId) return res.status(403).json({ success: false, message: 'Merchant profile not found' });

        const { rows } = await db.query(
            `INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, usage_limit, is_active, merchant_id, kitchen_id)
             VALUES ($1, $2, $3, $4, $5, true, $6, $7) RETURNING *`,
            [code.toUpperCase(), discount_type, discount_value, min_order_amount || 0, usage_limit || 100, vendorId || null, kitchenId || null]
        );

        res.status(201).json({ success: true, data: rows[0] });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

module.exports = {
  registerMerchant,
  createBulkOrders,
  createBulkOrdersSession,
  getBatches,
  getBatchStatus,
  getMyBatches,
  getSellerDashboard,
  getMerchantProfile,
  setupMerchantProfile,
  updateMerchantSettings,
  getIncomingOrders,
  updateOrderStatus,
  getReturnRequests,
  processReturnRequest,
  confirmReturnReceipt,
  getMerchantAnalytics,
  getMerchantBySlug,
  getMerchantCoupons,
  createMerchantCoupon
};
