const axios = require('axios');
const crypto = require('crypto');
const db = require('../config/db');

const PAYSTACK_SECRET = process.env.PAYSTACK_SECRET_KEY;

const walletService = require('../services/walletService');

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
    const rawAmount = parseFloat(amount || 0);
    const koboAmount = Math.round(rawAmount * 100);

    if (koboAmount < 100) {
        return res.status(400).json({ success: false, message: 'Invalid amount: Minimum is ₦1.00' });
    }

    // Fetch user details for metadata
    const userRes = await db.query("SELECT full_name, phone FROM users WHERE id = $1", [userId]);
    const user = userRes.rows[0];

    const payload = {
      amount: koboAmount,
      email,
      metadata: {
        quote_id,
        user_id: userId,
        recipient_name: user?.full_name,
        recipient_phone: user?.phone
      },
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

    res.status(200).json(response.data.data);
  } catch (error) {
    console.error('[Paystack] API Error:', error.response?.data || error.message);
    const detail = error.response?.data?.message || error.message;
    res.status(400).json({ success: false, message: `Paystack failed: ${detail}` });
  }
};

/**
 * Initializes a Paystack transaction for Collect-on-Delivery (CoD).
 */
const initializeCoDPayment = async (req, res) => {
    const { orderId } = req.params;

    try {
        const { rows } = await db.query("SELECT * FROM orders WHERE id = $1", [orderId]);
        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Order not found' });

        const order = rows[0];
        if (!order.collect_on_delivery_amount) {
            return res.status(400).json({ success: false, message: 'This mission does not have a CoD component' });
        }

        const payload = {
            amount: Math.round(parseFloat(order.collect_on_delivery_amount) * 100),
            email: 'billing@pikop.ng', // Use a generic email for recipient collection
            metadata: {
                order_id: order.id,
                collection_type: 'COD'
            },
            channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer']
        };

        const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET.trim()}` }
        });

        // Set status to pending
        await db.query("UPDATE orders SET collection_status = 'pending' WHERE id = $1", [orderId]);

        res.status(200).json({
            success: true,
            data: response.data.data
        });

    } catch (error) {
        console.error('[Paystack CoD] Error:', error.message);
        res.status(400).json({ success: false, message: 'Failed to initialize collection' });
    }
};

/**
 * Verifies Paystack Webhook and Activates Order.
 */
const handleWebhook = async (req, res) => {
  const secret = (PAYSTACK_SECRET || '').trim();
  // CRITICAL: Paystack signature requires the original RAW request body
  const payload = req.rawBody || JSON.stringify(req.body);
  const hash = crypto.createHmac('sha512', secret).update(payload).digest('hex');
  const receivedSig = req.headers['x-paystack-signature'];

  console.log('--- PAYSTACK WEBHOOK INBOUND ---');
  if (hash !== receivedSig) {
      console.warn('[Webhook] ERROR: Signature mismatch. Access denied.');
      return res.sendStatus(401);
  }

  const event = req.body;
  console.log(`[Webhook] Event: ${event.event} | Ref: ${event.data?.reference}`);

  if (event.event === 'charge.success') {
    const { reference, metadata, channel } = event.data;

    // Handle CoD Collection Webhook
    if (metadata.collection_type === 'COD') {
        try {
            await db.query(
                "UPDATE orders SET collection_status = 'collected', collection_payment_reference = $1, collection_method = $2 WHERE id = $3",
                [reference, channel, metadata.order_id]
            );

            const socketService = require('../services/socketService');
            socketService.getIO().to(`order_${metadata.order_id}`).emit("status_updated", {
                orderId: metadata.order_id,
                status: 'PAYMENT_RECEIVED'
            });

            await walletService.processCoDRemittance(metadata.order_id);
            console.log(`[Webhook] CoD Collected for Order ${metadata.order_id}`);
            return res.sendStatus(200);
        } catch (e) {
            console.error('[Webhook] CoD Update Error:', e.message);
            return res.sendStatus(500);
        }
    }

    console.log(`[Webhook] charge.success received. QuoteID: ${metadata?.quote_id} | Reference: ${reference}`);

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Fetch Quote
        const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1", [metadata.quote_id]);
        if (quoteRes.rows.length === 0) {
            console.error(`[Webhook] Quote ${metadata.quote_id} NOT FOUND in database.`);
            throw new Error('Quote not found');
        }
        const q = quoteRes.rows[0];

        // 2. Create Unified Order (DEFINITIVE ALIGNMENT)
        console.log('[Webhook] Activating mission...');
        const orderInsertRes = await client.query(
            `INSERT INTO orders (
                order_type, user_id, quote_id, status,
                item_description, size_tier,
                pickup_address, delivery_address,
                pickup_location, delivery_location,
                total_fare, payment_reference, payment_status, payment_channel,
                pickup_code_hash, delivery_code_hash,
                recipient_name, recipient_phone,
                pickup_display_summary, delivery_display_summary
            ) VALUES (
                'pickup_delivery', $1, $2, 'SEARCHING',
                $3, $4,
                $5, $6,
                $7, $8,
                $9, $10, 'PAID', $11,
                'v3_pending', 'v3_pending',
                $12, $13, $14, $15
            ) RETURNING id`,
            [
                metadata.user_id, q.id,
                q.item_description, q.size_tier,
                q.pickup_address, q.delivery_address,
                q.pickup_location, q.delivery_location,
                q.total_fare, reference, channel,
                metadata.recipient_name || 'Recipient',
                metadata.recipient_phone || '000',
                q.pickup_address.substring(0, 50),
                q.delivery_address.substring(0, 50)
            ]
        );

        await client.query('COMMIT');
        console.log(`[Webhook] Mission SUCCESS. OrderID: ${orderInsertRes.rows[0].id} created.`);
    } catch (e) {
        await client.query('ROLLBACK');
        console.error('❌ [Webhook] Order Activation CRITICAL FAILURE:', e.message);
    } finally {
        client.release();
    }
  }

  res.sendStatus(200);
};

module.exports = {
  initializePayment,
  initializeCoDPayment,
  handleWebhook
};
