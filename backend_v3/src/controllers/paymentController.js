const axios = require('axios');
const crypto = require('crypto');
const db = require('../config/db');

const PAYSTACK_SECRET = (process.env.PAYSTACK_SECRET_KEY || '').trim();

const walletService = require('../services/walletService');
const emailService = require('../services/emailService');
const fcmService = require('../services/fcmService');
const smsService = require('../services/smsService');

/**
 * Initializes a Paystack transaction.
 */
const initializePayment = async (req, res) => {
  try {
    const {
        quote_id, amount, email,
        item_price, delivery_fee, platform_fee_amount,
        fee_payer, seller_phone, payer_id
    } = req.body;
    const userId = req.user.id;

    if (!quote_id) {
        console.error('[Paystack] ERROR: Missing quote_id in request body');
        return res.status(400).json({ success: false, message: 'quote_id is required for activation' });
    }

    if (!PAYSTACK_SECRET || PAYSTACK_SECRET.includes('your_')) {
        console.error('[Paystack] ERROR: Missing or invalid secret key in .env');
        return res.status(500).json({ success: false, message: 'Payment gateway not configured' });
    }

    let koboAmount = Math.round(parseFloat(amount) * 100);

    if (koboAmount < 100) {
        return res.status(400).json({ success: false, message: 'Invalid amount: Minimum is ₦1.00' });
    }

    // Fetch user details for metadata
    const userRes = await db.query("SELECT full_name, phone FROM users WHERE id = $1", [userId]);
    const user = userRes.rows[0];

    const payload = {
      amount: koboAmount,
      email,
      currency: 'NGN',
      callback_url: 'https://api.pikop.com.ng/api/v1/payments/webhook', // Redirect through backend
      channels: ['card', 'bank', 'ussd', 'bank_transfer', 'qr', 'mobile_money'],
      metadata: {
        quote_id,
        user_id: userId,
        item_price: item_price || 0,
        delivery_fee: delivery_fee || 0,
        platform_fee_amount: platform_fee_amount || 0,
        fee_payer: fee_payer || 'PAYER',
        initiator_role: fee_payer || 'PAYER',
        seller_phone: seller_phone || null,
        payer_id: payer_id || null,
        recipient_name: user?.full_name,
        recipient_phone: user?.phone
      }
    };

    console.log(`[Paystack] Initializing for ${email}. Amount: ${payload.amount} kobo. Channels requested: ${payload.channels.join(', ')}`);

    const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
      headers: {
          Authorization: `Bearer ${PAYSTACK_SECRET}`,
          'Content-Type': 'application/json'
      },
      timeout: 10000
    });

    if (response.data.status) {
        console.log(`[Paystack] Initialization SUCCESS. Reference: ${response.data.data.reference}`);
    } else {
        console.warn(`[Paystack] Initialization returned false status:`, response.data);
    }

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
            currency: 'NGN',
            callback_url: 'https://api.pikop.com.ng/api/v1/payments/webhook',
            channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer'],
            metadata: {
                order_id: order.id,
                collection_type: 'COD'
            }
        };

        console.log(`[Paystack CoD] Initializing. Order: ${order.id}. Amount: ${payload.amount} kobo`);

        const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
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
 * Shared logic to activate a mission after successful payment.
 */
const activatePaidMission = async (client, metadata, reference, channel) => {
    console.log(`[Activation] Triggering for Quote: ${metadata.quote_id} | Ref: ${reference}`);

    // 1. Fetch Quote with explicit coordinates
    const quoteRes = await client.query(
        `SELECT *,
         ST_Y(pickup_location::geometry) as p_lat, ST_X(pickup_location::geometry) as p_lng,
         ST_Y(delivery_location::geometry) as d_lat, ST_X(delivery_location::geometry) as d_lng
         FROM quotes WHERE id = $1`,
        [metadata.quote_id]
    );

    if (quoteRes.rows.length === 0) {
        throw new Error(`Quote ${metadata.quote_id} not found`);
    }
    const q = quoteRes.rows[0];

    // 2. Insert Unified Order
    const orderInsertRes = await client.query(
        `INSERT INTO orders (
            order_type, user_id, quote_id, status,
            item_description, size_tier,
            pickup_address, delivery_address,
            pickup_location, delivery_location,
            total_fare, payment_reference, payment_status, payment_channel, payment_method,
            pickup_code_hash, delivery_code_hash,
            recipient_name, recipient_phone,
            pickup_display_summary, delivery_display_summary,
            item_price, delivery_fee, platform_fee_amount, fee_payer, initiator_role,
            escrow_status, seller_phone, payer_id
        ) VALUES (
            'pickup_delivery', $1, $2, 'PAYMENT_CAPTURED',
            $3, $4,
            $5, $6,
            ST_SetSRID(ST_MakePoint($7, $8), 4326)::geography,
            ST_SetSRID(ST_MakePoint($9, $10), 4326)::geography,
            $11, $12, 'PAID', $13, $14,
            'v3_pending', 'v3_pending',
            $15, $16, $17, $18,
            $19, $20, $21, $22, $23,
            $24, $25, $26
        ) RETURNING id`,
        [
            metadata.user_id, q.id,
            q.item_description, q.size_tier,
            q.pickup_address, q.delivery_address,
            q.p_lng, q.p_lat, q.d_lng, q.d_lat,
            q.total_fare, reference, channel, channel,
            metadata.recipient_name || 'Recipient',
            metadata.recipient_phone || '000',
            q.pickup_address.substring(0, 50),
            q.delivery_address.substring(0, 50),
            parseFloat(metadata.item_price || 0),
            parseFloat(metadata.delivery_fee || 0),
            parseFloat(metadata.platform_fee_amount || 0),
            metadata.fee_payer || 'PAYER',
            metadata.initiator_role || 'PAYER',
            (parseFloat(metadata.item_price || 0) > 0) ? 'held' : 'not_applicable',
            metadata.seller_phone || null,
            metadata.payer_id || null
        ]
    );

    const orderId = orderInsertRes.rows[0].id;
    console.log(`[Activation] SUCCESS. Mission ${orderId} is now active.`);

    // 3. Handle Escrow Ledger
    if (parseFloat(metadata.item_price || 0) > 0) {
        try {
            let sellerUserId = null;
            if (metadata.seller_phone) {
                const sRes = await client.query("SELECT id FROM users WHERE phone = $1", [metadata.seller_phone]);
                if (sRes.rows.length > 0) sellerUserId = sRes.rows[0].id;
            }

            if (sellerUserId) {
                await client.query("UPDATE orders SET seller_id = $1 WHERE id = $2", [sellerUserId, orderId]);
                const sellerWalletId = await walletService.ensureWalletExists(client, 'USER', sellerUserId);
                await walletService.recordEntry(
                    client, sellerWalletId, 'CREDIT', parseFloat(metadata.item_price),
                    'ESCROW_HOLD', `Escrow held for Order #${orderId}`,
                    orderId, 'pending',
                    {
                        item_price: metadata.item_price,
                        delivery_fee: metadata.delivery_fee,
                        fee: metadata.platform_fee_amount,
                        fee_payer: metadata.fee_payer
                    }
                );
            }
        } catch (escrowErr) {
            console.error('[Activation] Escrow Ledger Error:', escrowErr.message);
        }

        // 4. Outreach
        if (metadata.payer_id) {
            fcmService.sendNotification(metadata.payer_id, "Secure Pay Request", `A Secure Pay request for ₦${metadata.item_price} is waiting for your payment.`, { type: "SECURE_PAY_REQUEST", order_id: orderId.toString() });
        } else {
            smsService.sendSecurePaySms(metadata.recipient_phone, metadata.item_price, orderId).catch(e => {});
        }
    }

    // 5. Trigger Receipt Email
    try {
        const { rows: uRes } = await client.query("SELECT email, full_name FROM users WHERE id = $1", [metadata.user_id]);
        if (uRes.length > 0) {
            emailService.sendPaymentReceiptEmail(uRes[0].email, uRes[0].full_name, orderId, q.total_fare, q.item_description)
                .catch(e => console.error('[Activation] Email error:', e.message));
        }
    } catch (e) {}

    return orderId;
};

/**
 * Verifies Paystack Webhook and Activates Order.
 */
const handleWebhook = async (req, res) => {
  const secret = (PAYSTACK_SECRET || '').trim();
  const payload = req.rawBody || JSON.stringify(req.body);
  const hash = crypto.createHmac('sha512', secret).update(payload).digest('hex');
  const receivedSig = req.headers['x-paystack-signature'];

  console.log('--- PAYSTACK WEBHOOK INBOUND ---');
  if (hash !== receivedSig) {
      console.warn('[Webhook] ERROR: Signature mismatch. Access denied.');
      return res.sendStatus(401);
  }

  const event = req.body;
  const data = event.data;
  console.log(`[Webhook] Event: ${event.event} | Ref: ${data?.reference}`);

  if (event.event === 'charge.success') {
    const { reference, metadata, channel } = data;

    // 1. Handle CoD Collection
    if (metadata?.collection_type === 'COD') {
        const orderId = metadata.order_id;
        await db.query(
            "UPDATE orders SET collection_status = 'collected', payment_status = 'PAID', collection_payment_reference = $1, collection_method = $2 WHERE id = $3",
            [reference, channel, orderId]
        );
        console.log(`[Webhook] CoD payment successful for Order ${orderId}`);
        try {
            await walletService.processCoDRemittance(orderId);
        } catch (e) {
            console.error('[Webhook] CoD Remittance Error:', e.message);
        }
        return res.sendStatus(200);
    }

    // 2. Handle Wallet Top-up
    if (metadata?.type === 'TOPUP') {
        const amount = data.amount / 100;
        const userId = metadata.user_id;
        const client = await db.pool.connect();
        try {
            await client.query('BEGIN');
            const walletId = await walletService.ensureWalletExists(client, 'USER', userId);
            await walletService.recordEntry(
                client, walletId, 'CREDIT', amount, 'TOPUP',
                `Wallet Top-up via Paystack. Ref: ${reference}`, null, 'available'
            );
            await client.query('COMMIT');
            console.log(`[Webhook] Top-up SUCCESS. User ${userId} credited with ₦${amount}`);
        } catch (e) {
            await client.query('ROLLBACK');
            console.error('[Webhook] Top-up FAILED:', e.message);
        } finally {
            client.release();
        }
        return res.sendStatus(200);
    }

    // 3. Handle Mission Activation
    const existingOrder = await db.query("SELECT id FROM orders WHERE payment_reference = $1", [reference]);
    if (existingOrder.rows.length > 0) {
        console.log(`[Webhook] Reference ${reference} already processed.`);
        return res.sendStatus(200);
    }

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        await activatePaidMission(client, metadata, reference, channel);
        await client.query('COMMIT');
    } catch (e) {
        await client.query('ROLLBACK');
        console.error('❌ [Webhook] Order Activation FAILED:', e.message);
    } finally {
        client.release();
    }
  }

  // 4. Handle Payout Transfers
  if (event.event === 'transfer.success') {
      const { transfer_code, reference } = data;
      await db.query(
          "UPDATE withdrawals SET status = 'SUCCESSFUL', processed_at = CURRENT_TIMESTAMP WHERE paystack_transfer_code = $1",
          [transfer_code]
      );
      console.log(`[Webhook] Payout SUCCESS for code: ${transfer_code}`);
  }

  if (event.event === 'transfer.failed' || event.event === 'transfer.reversed') {
      const { transfer_code } = data;
      const client = await db.pool.connect();
      try {
          await client.query('BEGIN');
          const wRes = await client.query(
              "UPDATE withdrawals SET status = 'REVERSED', processed_at = CURRENT_TIMESTAMP WHERE paystack_transfer_code = $1 RETURNING wallet_id, amount",
              [transfer_code]
          );
          if (wRes.rows.length > 0) {
              const { wallet_id, amount: wAmount } = wRes.rows[0];
              await walletService.recordEntry(client, wallet_id, 'CREDIT', wAmount, 'WITHDRAWAL_REVERSAL', `Payout failed/reversed: ${transfer_code}`);
          }
          await client.query('COMMIT');
          console.log(`[Webhook] Payout FAILED/REVERSED. Code: ${transfer_code}`);
      } catch (e) {
          await client.query('ROLLBACK');
          console.error('[Webhook] Transfer Reversal Error:', e.message);
      } finally {
          client.release();
      }
  }

  res.sendStatus(200);
};

/**
 * Redirects the user back to the app after a successful Paystack payment.
 */
const handleWebhookGET = (req, res) => {
    const { reference } = req.query;
    console.log(`[Paystack Redirect] Returning to app. Reference: ${reference}`);
    res.send(`
        <!DOCTYPE html>
        <html>
        <body>
            <script>
                window.location.href = "pikop://payment/success?reference=${reference}";
                setTimeout(() => {
                    window.location.href = "intent://payment/success?reference=${reference}#Intent;scheme=pikop;package=com.ng.pikop;end";
                }, 1000);
            </script>
            <p>Redirecting back to Pikop...</p>
            <a href="pikop://payment/success?reference=${reference}">Click here if not redirected</a>
        </body>
        </html>
    `);
};

/**
 * Verifies a transaction reference directly with Paystack.
 */
const verifyPayment = async (req, res) => {
    const { reference } = req.params;
    if (!reference) return res.status(400).json({ success: false, message: 'Reference required' });

    try {
        console.log(`[Paystack] Verifying reference: ${reference}`);
        const response = await axios.get(`https://api.paystack.co/transaction/verify/${reference}`, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
        });

        const tx = response.data.data;
        if (tx.status === 'success') {
            const metadata = tx.metadata || {};

            if (metadata.type === 'TOPUP') {
                const amount = tx.amount / 100;
                const userId = metadata.user_id;
                const client = await db.pool.connect();
                try {
                    await client.query('BEGIN');
                    const existingLedger = await client.query("SELECT id FROM wallet_ledger_entries WHERE metadata->>'reference' = $1", [reference]);
                    if (existingLedger.rows.length === 0) {
                        const walletId = await walletService.ensureWalletExists(client, 'USER', userId);
                        await walletService.recordEntry(client, walletId, 'CREDIT', amount, 'TOPUP', `Wallet Top-up (verified). Ref: ${reference}`, null, 'available', { reference });
                    }
                    await client.query('COMMIT');
                } catch (e) {
                    await client.query('ROLLBACK');
                    console.error('[Verify] Top-up FAILED:', e.message);
                } finally {
                    client.release();
                }
                return res.status(200).json({ success: true, status: 'PAID' });
            }

            const client = await db.pool.connect();
            try {
                await client.query('BEGIN');
                const existing = await client.query("SELECT id FROM orders WHERE payment_reference = $1", [reference]);
                if (existing.rows.length === 0) {
                    await activatePaidMission(client, metadata, reference, tx.channel);
                }
                await client.query('COMMIT');
            } catch (e) {
                await client.query('ROLLBACK');
                console.error('[Verify] Activation FAILED:', e.message);
            } finally {
                client.release();
            }
            return res.status(200).json({ success: true, status: 'PAID' });
        }
        return res.status(200).json({ success: false, status: tx.status });
    } catch (error) {
        console.error('[Paystack Verify] Error:', error.message);
        res.status(500).json({ success: false, message: 'Verification failed' });
    }
};

module.exports = {
  initializePayment,
  initializeCoDPayment,
  handleWebhook,
  handleWebhookGET,
  verifyPayment
};
