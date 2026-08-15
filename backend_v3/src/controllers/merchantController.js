const db = require('../config/db');
const crypto = require('crypto');

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

module.exports = {
  registerMerchant,
  createBulkOrders,
  getBatches
};
