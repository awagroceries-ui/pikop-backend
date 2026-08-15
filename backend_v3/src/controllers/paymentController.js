const axios = require('axios');
const crypto = require('crypto');
const db = require('../config/db');

const PAYSTACK_SECRET = process.env.PAYSTACK_SECRET_KEY;

/**
 * Initializes a Paystack transaction.
 */
const initializePayment = async (req, res) => {
  const { quote_id, amount, email } = req.body;
  const userId = req.user.id;

  if (!PAYSTACK_SECRET) throw new Error('Paystack not configured');

  try {
    const response = await axios.post('https://api.paystack.co/transaction/initialize', {
      amount: Math.round(amount * 100), // Naira to Kobo
      email,
      metadata: { quote_id, user_id: userId },
      channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer']
    }, {
      headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
    });

    res.status(200).json({
      success: true,
      data: response.data.data
    });
  } catch (error) {
    console.error('[Paystack] Init Error:', error.response?.data || error.message);
    res.status(400).json({ success: false, message: 'Could not initialize payment' });
  }
};

/**
 * Verifies Paystack Webhook and Activates Order.
 */
const handleWebhook = async (req, res) => {
  const hash = crypto.createHmac('sha512', PAYSTACK_SECRET).update(JSON.stringify(req.body)).digest('hex');
  if (hash !== req.headers['x-paystack-signature']) return res.sendStatus(401);

  const event = req.body;
  if (event.event === 'charge.success') {
    const { reference, metadata, channel } = event.data;
    const client = await db.pool.connect();

    try {
        await client.query('BEGIN');

        // 1. Fetch Quote
        const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1", [metadata.quote_id]);
        if (quoteRes.rows.length === 0) throw new Error('Quote not found');
        const q = quoteRes.rows[0];

        // 2. Persist Addresses (Master Brief requires ID linkage)
        const pAddr = await client.query(
            "INSERT INTO addresses (user_id, formatted_address, location, landmark_description) VALUES ($1, $2, $3, $4) RETURNING id",
            [metadata.user_id, q.pickup_address, q.pickup_location, 'Standard Pickup']
        );
        const dAddr = await client.query(
            "INSERT INTO addresses (user_id, formatted_address, location, landmark_description) VALUES ($1, $2, $3, $4) RETURNING id",
            [metadata.user_id, q.delivery_address, q.delivery_location, 'Standard Delivery']
        );

        // 3. Create Unified Order (v3)
        await client.query(
            `INSERT INTO orders (
                order_type, user_id, quote_id, status,
                item_description, size_tier,
                pickup_address_id, delivery_address_id,
                pickup_address, delivery_address,
                pickup_location, delivery_location,
                total_fare, payment_reference, payment_status, payment_channel, payment_method,
                pickup_code_hash, delivery_code_hash
            ) VALUES (
                'pickup_delivery', $1, $2, 'SEARCHING',
                $3, $4,
                $5, $6,
                $7, $8,
                $9, $10,
                $11, $12, 'PAID', $13, 'online',
                $14, $15
            )`,
            [
                metadata.user_id, q.id,
                q.item_description, q.size_tier,
                pAddr.rows[0].id, dAddr.rows[0].id,
                q.pickup_address, q.delivery_address,
                q.pickup_location, q.delivery_location,
                q.total_fare, reference, channel,
                'v3_mock_hash', 'v3_mock_hash' // Will implement real hashing in next step
            ]
        );

        await client.query('COMMIT');
        console.log(`[Webhook] Mission Activated: ${reference}`);
    } catch (e) {
        await client.query('ROLLBACK');
        console.error('[Webhook] V3 Activation Error:', e.message);
    } finally {
        client.release();
    }
  }

  res.sendStatus(200);
};

module.exports = {
  initializePayment,
  handleWebhook
};
