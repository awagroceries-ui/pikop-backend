const axios = require('axios');
const crypto = require('crypto');
const db = require('../config/db');

const PAYSTACK_SECRET = process.env.PAYSTACK_SECRET_KEY;

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
      callback_url: 'pikop://payment/success',
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

    console.log(`[Paystack] Initializing for ${email}. Amount: ${payload.amount} kobo. Quote: ${quote_id}`);
    console.log(`[Paystack] FULL PAYLOAD:`, JSON.stringify(payload));

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
            currency: 'NGN',
            callback_url: 'pikop://payment/success',
            metadata: {
                order_id: order.id,
                collection_type: 'COD'
            }
        };

        console.log(`[Paystack CoD] Initializing. Order: ${order.id}. Amount: ${payload.amount} kobo`);
        console.log(`[Paystack CoD] FULL PAYLOAD:`, JSON.stringify(payload));

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

  // --- TRANSFER EVENTS (Automated Payouts) ---
  if (event.event === 'transfer.success') {
      const { transfer_code } = event.data;
      const { rows } = await db.query(
          "UPDATE withdrawals SET status = 'SUCCESSFUL', processed_at = CURRENT_TIMESTAMP WHERE paystack_transfer_code = $1 RETURNING fulfiller_id, amount",
          [transfer_code]
      );

      if (rows.length > 0) {
          const { fulfiller_id, amount: wAmount } = rows[0];
          const uRes = await db.query("SELECT user_id FROM fulfillers WHERE id = $1", [fulfiller_id]);
          if (uRes.rows.length > 0) {
              fcmService.sendPayoutAlert(uRes.rows[0].user_id, wAmount).catch(e => {});
          }
      }

      console.log(`[Webhook] Transfer SUCCESS for code: ${transfer_code}`);
      return res.sendStatus(200);
  }

  if (event.event === 'transfer.failed' || event.event === 'transfer.reversed') {
      const { transfer_code } = event.data;
      const client = await db.pool.connect();
      try {
          await client.query('BEGIN');

          // 1. Mark Withdrawal as FAILED/REVERSED
          const wRes = await client.query(
              "UPDATE withdrawals SET status = 'REVERSED', processed_at = CURRENT_TIMESTAMP WHERE paystack_transfer_code = $1 RETURNING wallet_id, amount",
              [transfer_code]
          );

          if (wRes.rows.length > 0) {
              const { wallet_id, amount: wAmount } = wRes.rows[0];
              // 2. Re-credit the Fulfiller's wallet
              await walletService.recordEntry(
                  client, wallet_id, 'CREDIT', wAmount,
                  'WITHDRAWAL_REVERSAL', `Payout failed/reversed: ${transfer_code}`
              );
          }

          await client.query('COMMIT');
          console.log(`[Webhook] Transfer FAILED/REVERSED. Funds returned to wallet for code: ${transfer_code}`);
      } catch (e) {
          await client.query('ROLLBACK');
          console.error('[Webhook] Transfer Reversal Error:', e.message);
      } finally {
          client.release();
      }
      return res.sendStatus(200);
  }

  if (event.event === 'charge.success') {
    const { reference, metadata, channel } = event.data;

    // 0. IDEMPOTENCY CHECK: Has this reference already been processed?
    const existingOrder = await db.query(
        "SELECT id FROM orders WHERE payment_reference = $1",
        [reference]
    );
    if (existingOrder.rows.length > 0) {
        console.log(`[Webhook] Reference ${reference} already processed. Skipping.`);
        return res.sendStatus(200);
    }

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

    // Handle Wallet Top-up Webhook
    if (metadata.type === 'TOPUP') {
        const client = await db.pool.connect();
        try {
            await client.query('BEGIN');

            const walletId = await walletService.ensureWalletExists(client, 'USER', metadata.user_id);
            const amount = event.data.amount / 100; // Convert Kobo to Naira

            await walletService.recordEntry(
                client, walletId, 'CREDIT', amount,
                'WALLET_TOPUP', `Top-up via ${channel}`,
                null, 'available', { reference }
            );

            await client.query('COMMIT');
            console.log(`[Webhook] Wallet TOPUP SUCCESS for User ${metadata.user_id}: ₦${amount}`);
            return res.sendStatus(200);
        } catch (e) {
            await client.query('ROLLBACK');
            console.error('[Webhook] Top-up Error:', e.message);
            return res.sendStatus(500);
        } finally {
            client.release();
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
        console.log('[Webhook] Attempting to activate mission...');
        try {
            const orderInsertRes = await client.query(
                `INSERT INTO orders (
                    order_type, user_id, quote_id, status,
                    item_description, size_tier,
                    pickup_address, delivery_address,
                    pickup_location, delivery_location,
                    total_fare, payment_reference, payment_status, payment_channel,
                    pickup_code_hash, delivery_code_hash,
                    recipient_name, recipient_phone,
                    pickup_display_summary, delivery_display_summary,
                    item_price, delivery_fee, platform_fee_amount, fee_payer, initiator_role,
                    escrow_status, seller_phone
                ) VALUES (
                    'pickup_delivery', $1, $2, 'PAYMENT_CAPTURED',
                    $3, $4,
                    $5, $6,
                    $7, $8,
                    $9, $10, 'PAID', $11,
                    'v3_pending', 'v3_pending',
                    $12, $13, $14, $15,
                    $16, $17, $18, $19, $20,
                    $21, $22
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
                    q.delivery_address.substring(0, 50),
                    metadata.item_price || 0,
                    metadata.delivery_fee || 0,
                    metadata.platform_fee_amount || 0,
                    metadata.fee_payer || 'PAYER',
                    metadata.initiator_role || 'PAYER',
                    (metadata.item_price > 0) ? 'held' : 'not_applicable',
                    metadata.seller_phone || null,
                    metadata.payer_id || null
                ]
            );
            const orderId = orderInsertRes.rows[0].id;
            console.log(`[Webhook] SUCCESS. Mission ${orderId} is now active.`);

            // 3. Escrow Hold Ledger & Pending Balance (if item price exists)
            if (metadata.item_price > 0) {
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
                            client, sellerWalletId, 'CREDIT', metadata.item_price,
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
                    console.error('[Webhook] Escrow Ledger Error:', escrowErr.message);
                }

                // 4. Outreach (Push vs SMS)
                if (metadata.payer_id) {
                    fcmService.sendNotification(metadata.payer_id, "Secure Pay Request", `A Secure Pay request for ₦${metadata.item_price} is waiting for your payment.`, { type: "SECURE_PAY_REQUEST", order_id: orderId.toString() });
                } else {
                    smsService.sendSecurePaySms(metadata.recipient_phone, metadata.item_price, orderId).catch(e => {});
                }
            }

            // Trigger Branded Payment Receipt Email
            try {
                const { rows: uRes } = await client.query("SELECT email, full_name FROM users WHERE id = $1", [metadata.user_id]);
                if (uRes.length > 0) {
                    emailService.sendPaymentReceiptEmail(uRes[0].email, uRes[0].full_name, orderInsertRes.rows[0].id, q.total_fare, q.item_description)
                        .catch(e => console.error('[WebhookReceipt] Email error:', e.message));
                }
            } catch (e) {}
        } catch (dbError) {
            console.error('[Webhook] DB INSERT ERROR:', dbError.message);
            throw dbError; // Trigger rollback
        }

        await client.query('COMMIT');
    } catch (e) {
        await client.query('ROLLBACK');
        console.error('❌ [Webhook] Order Activation CRITICAL FAILURE:', e.message);
    } finally {
        client.release();
    }
  }

  res.sendStatus(200);
};

/**
 * Friendly redirect for browser-based webhook GET requests.
 */
const handleWebhookGET = (req, res) => {
    const intentUrl = "intent://payment/success#Intent;scheme=pikop;package=com.ng.pikop;end";
    const directUrl = "pikop://payment/success";

    res.send(`
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Payment Success | Pikop</title>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;700;800&display=swap');
                body { font-family: 'Inter', -apple-system, sans-serif; background-color: #F3F4F6; margin: 0; padding: 0; display: flex; align-items: center; justify-content: center; height: 100vh; }
                .card { background: white; padding: 60px 40px; border-radius: 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1); max-width: 400px; width: 90%; text-align: center; }
                .success-icon { width: 80px; height: 80px; background: #008751; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 40px; margin: 0 auto 32px; }
                h1 { font-size: 24px; font-weight: 800; color: #111827; margin-bottom: 12px; letter-spacing: -0.5px; }
                p { font-size: 16px; color: #6B7280; line-height: 1.6; margin-bottom: 40px; }
                .btn { display: block; width: 100%; padding: 18px 0; background: #008751; color: white; border: none; border-radius: 16px; font-weight: 700; text-decoration: none; font-size: 16px; transition: all 0.2s; box-shadow: 0 10px 15px -3px rgba(0, 135, 81, 0.3); }
                .btn:hover { background: #006b3f; transform: translateY(-2px); }
                .fallback-link { display: block; margin-top: 24px; color: #6B7280; font-size: 13px; text-decoration: none; font-weight: 600; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="success-icon">✓</div>
                <h1>Mission Activated!</h1>
                <p>Payment confirmed. You are being redirected back to your mission control.</p>
                <a href="${intentUrl}" class="btn">RETURN TO APP</a>
                <a href="${directUrl}" class="fallback-link">Click here if not redirected</a>
            </div>
            <script>
                // Automated force redirect
                const autoRedirect = () => {
                    window.location.replace("${intentUrl}");
                    setTimeout(() => {
                        window.location.href = "${directUrl}";
                    }, 500);
                };

                setTimeout(autoRedirect, 1000);
            </script>
        </body>
        </html>
    `);
};

/**
 * Verifies a transaction reference directly with Paystack.
 * Source of Truth for the Android client.
 */
const verifyPayment = async (req, res) => {
    const { reference } = req.params;
    if (!reference) return res.status(400).json({ success: false, message: 'Reference required' });

    try {
        console.log(`[Paystack] Verifying reference: ${reference}`);
        const response = await axios.get(`https://api.paystack.co/transaction/verify/${reference}`, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET.trim()}` }
        });

        const tx = response.data.data;
        if (tx.status === 'success') {
            const metadata = tx.metadata || {};

            // Handle Top-Up Verification
            if (metadata.type === 'TOPUP') {
                const client = await db.pool.connect();
                try {
                    await client.query('BEGIN');
                    const existingLedger = await client.query(
                        "SELECT id FROM wallet_ledger_entries WHERE metadata->>'reference' = $1",
                        [reference]
                    );
                    if (existingLedger.rows.length === 0) {
                        const walletId = await walletService.ensureWalletExists(client, 'USER', metadata.user_id);
                        const amount = tx.amount / 100; // Convert Kobo to Naira
                        await walletService.recordEntry(
                            client, walletId, 'CREDIT', amount,
                            'WALLET_TOPUP', `Top-up via ${tx.channel} (verified)`,
                            null, 'available', { reference }
                        );
                        console.log(`[Verify] Wallet TOPUP SUCCESS for User ${metadata.user_id}: ₦${amount}`);
                    }
                    await client.query('COMMIT');
                } catch (e) {
                    await client.query('ROLLBACK');
                    console.error('[Verify] Top-up Credit Error:', e.message);
                } finally {
                    client.release();
                }
                return res.status(200).json({ success: true, status: 'PAID' });
            }

            const { quote_id, user_id, recipient_name, recipient_phone } = metadata;

            // Trigger activation logic (mirroring Webhook for robustness)
            const client = await db.pool.connect();
            try {
                await client.query('BEGIN');

                // Check if already active
                const existing = await client.query("SELECT id FROM orders WHERE payment_reference = $1", [reference]);
                if (existing.rows.length === 0) {
                    const quoteRes = await client.query("SELECT * FROM quotes WHERE id = $1", [quote_id]);
                    if (quoteRes.rows.length > 0) {
                        const q = quoteRes.rows[0];
                        await client.query(
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
                            )`,
                            [
                                user_id, q.id, q.item_description, q.size_tier,
                                q.pickup_address, q.delivery_address,
                                q.pickup_location, q.delivery_location,
                                q.total_fare, reference, tx.channel,
                                recipient_name || 'Recipient',
                                recipient_phone || '000',
                                q.pickup_address.substring(0, 50),
                                q.delivery_address.substring(0, 50)
                            ]
                        );
                        console.log(`[Verify] Mission activated via client-triggered verification: ${reference}`);

                        try {
                            const { rows: uRes } = await client.query("SELECT email, full_name FROM users WHERE id = $1", [user_id]);
                            if (uRes.length > 0) {
                                emailService.sendPaymentReceiptEmail(uRes[0].email, uRes[0].full_name, q.id, q.total_fare, q.item_description)
                                    .catch(e => console.error('[VerifyReceipt] Email error:', e.message));
                            }
                        } catch (e) {}
                    }
                }
                await client.query('COMMIT');
            } catch (e) {
                await client.query('ROLLBACK');
                console.error('[Verify] DB Error:', e.message);
            } finally {
                client.release();
            }

            return res.status(200).json({ success: true, status: 'PAID' });
        } else {
            return res.status(200).json({ success: false, status: tx.status });
        }

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
