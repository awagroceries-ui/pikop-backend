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

  if (!PAYSTACK_SECRET || PAYSTACK_SECRET.includes('your_')) {
      console.error('[Paystack] ERROR: Missing or invalid secret key in .env');
      return res.status(500).json({ success: false, message: 'Payment gateway not configured' });
  }

  try {
    const payload = {
      amount: Math.round(parseFloat(amount) * 100), // Ensure it's Kobo and an integer
      email,
      metadata: { quote_id, user_id: userId },
      channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer']
    };

    console.log(`[Paystack] Initializing for ${email}. Amount: ${payload.amount} kobo. Quote: ${quote_id}`);

    const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
      headers: {
          Authorization: `Bearer ${PAYSTACK_SECRET.trim()}`,
          'Content-Type': 'application/json'
      },
      timeout: 10000
    });

    res.status(200).json({
      success: true,
      data: response.data.data
    });
  } catch (error) {
    console.error('[Paystack] API Error:', error.response?.data || error.message);
    const detail = error.response?.data?.message || error.message;
    res.status(400).json({ success: false, message: `Paystack failed: ${detail}` });
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
    const { reference, metadata, channel, paid_at } = event.data;
    const client = await db.pool.connect();

    try {
        await client.query('BEGIN');

        // 1. Fetch Quote
        const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1", [metadata.quote_id]);
        if (quoteRes.rows.length === 0) throw new Error('Quote not found');
        const q = quoteRes.rows[0];

        // 2. Persist Addresses
        const pAddr = await client.query(
            "INSERT INTO addresses (user_id, formatted_address, location, landmark_description) VALUES ($1, $2, $3, 'Standard Pickup') RETURNING id",
            [metadata.user_id, q.pickup_address, q.pickup_location]
        );
        const dAddr = await client.query(
            "INSERT INTO addresses (user_id, formatted_address, location, landmark_description) VALUES ($1, $2, $3, 'Standard Delivery') RETURNING id",
            [metadata.user_id, q.delivery_address, q.delivery_location]
        );

        // 3. Create Unified Order
        await client.query(
            `INSERT INTO orders (
                order_type, user_id, quote_id, status,
                item_description, size_tier,
                pickup_address_id, delivery_address_id,
                pickup_address, delivery_address,
                pickup_location, delivery_location,
                total_fare, payment_reference, payment_status, payment_channel,
                pickup_code_hash, delivery_code_hash
            ) VALUES (
                'pickup_delivery', $1, $2, 'SEARCHING',
                $3, $4,
                $5, $6,
                $7, $8,
                $9, $10,
                $11, $12, 'PAID', $13,
                $14, $15
            )`,
            [
                metadata.user_id, q.id,
                q.item_description, q.size_tier,
                pAddr.rows[0].id, dAddr.rows[0].id,
                q.pickup_address, q.delivery_address,
                q.pickup_location, q.delivery_location,
                q.total_fare, reference, channel,
                'v3_pending_hash', 'v3_pending_hash'
            ]
        );

        await client.query('COMMIT');
        console.log(`[Webhook] Mission Activated: ${reference}`);
    } catch (e) {
        await client.query('ROLLBACK');
        console.error('[Webhook] Activation Failure:', e.message);
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
