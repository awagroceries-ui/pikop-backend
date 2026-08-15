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
 * Verifies Paystack Webhook.
 */
const handleWebhook = async (req, res) => {
  const hash = crypto.createHmac('sha512', PAYSTACK_SECRET).update(JSON.stringify(req.body)).digest('hex');
  if (hash !== req.headers['x-paystack-signature']) return res.sendStatus(401);

  const event = req.body;
  if (event.event === 'charge.success') {
    const { reference, metadata, channel, paid_at } = event.data;

    // Mission Activation Logic
    try {
        const quoteRes = await db.query("SELECT * FROM quotes WHERE id = $1", [metadata.quote_id]);
        if (quoteRes.rows.length > 0) {
            const q = quoteRes.rows[0];
            await db.query(
                `INSERT INTO orders (user_id, quote_id, pickup_address, delivery_address, pickup_location, delivery_location, total_fare, payment_reference, payment_status, payment_channel, paid_at)
                 VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'PAID', $9, $10)`,
                [metadata.user_id, q.id, q.pickup_address, q.delivery_address, q.pickup_location, q.delivery_location, q.total_fare, reference, channel, paid_at]
            );
            console.log(`[Webhook] Mission Activated for Quote ${metadata.quote_id}`);
        }
    } catch (e) {
        console.error('[Webhook] DB Error:', e.message);
    }
  }

  res.sendStatus(200);
};

module.exports = {
  initializePayment,
  handleWebhook
};
