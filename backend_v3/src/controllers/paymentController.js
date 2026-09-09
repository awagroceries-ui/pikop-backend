const axios = require('axios');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
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
        fee_payer, seller_phone, payer_id, promo_id
    } = req.body;
    const userId = req.user.id;

    if (!quote_id) return res.status(400).json({ success: false, message: 'quote_id is required' });

    if (!PAYSTACK_SECRET || PAYSTACK_SECRET.includes('your_')) {
        return res.status(500).json({ success: false, message: 'Payment gateway not configured' });
    }

    let koboAmount = Math.round(parseFloat(amount) * 100);
    if (koboAmount < 100) return res.status(400).json({ success: false, message: 'Invalid amount' });

    const userRes = await db.query("SELECT full_name, phone FROM users WHERE id = $1", [userId]);
    const user = userRes.rows[0];

    const payload = {
      amount: koboAmount,
      email,
      currency: 'NGN',
      callback_url: 'https://api.pikop.com.ng/api/v1/payments/webhook', // Browser Redirect Target
      channels: ['card', 'bank', 'ussd', 'bank_transfer', 'qr', 'mobile_money'],
      metadata: {
        quote_id,
        user_id: userId,
        item_price: parseFloat(item_price || 0),
        delivery_fee: parseFloat(delivery_fee || 0),
        platform_fee_amount: parseFloat(platform_fee_amount || 0),
        fee_payer: fee_payer || 'PAYER',
        initiator_role: fee_payer || 'PAYER',
        seller_phone: seller_phone || null,
        payer_id: payer_id || null,
        promo_id: promo_id || null,
        recipient_name: user?.full_name,
        recipient_phone: user?.phone
      }
    };

    console.log(`[Paystack] Initializing for ${email}. Quote: ${quote_id}`);

    const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
      headers: { Authorization: `Bearer ${PAYSTACK_SECRET}`, 'Content-Type': 'application/json' },
      timeout: 10000
    });

    res.status(200).json(response.data.data);
  } catch (error) {
    console.error('[Paystack] Init Error:', error.response?.data || error.message);
    res.status(400).json({ success: false, message: `Paystack failed: ${error.message}` });
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
        const payload = {
            amount: Math.round(parseFloat(order.collect_on_delivery_amount) * 100),
            email: 'billing@pikop.ng',
            currency: 'NGN',
            callback_url: 'https://api.pikop.com.ng/api/v1/payments/webhook',
            channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer'],
            metadata: { order_id: order.id, collection_type: 'COD' }
        };

        const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
        });

        await db.query("UPDATE orders SET collection_status = 'pending' WHERE id = $1", [orderId]);
        res.status(200).json(response.data.data);
    } catch (error) {
        res.status(400).json({ success: false, message: 'Failed to initialize collection' });
    }
};

/**
 * Robust activation logic shared by Webhook and Direct Verify.
 */
const activatePaidMission = async (client, metadata, reference, channel) => {
    // 1. Handle Metadata parsing (Paystack sometimes sends stringified JSON)
    const m = typeof metadata === 'string' ? JSON.parse(metadata) : metadata;
    const qId = m.quote_id;

    console.log(`[Activation] Starting for Quote: ${qId} | Ref: ${reference}`);

    // 2. Fetch Quote Coordinates
    const quoteRes = await client.query(
        `SELECT *, ST_Y(pickup_location::geometry) as p_lat, ST_X(pickup_location::geometry) as p_lng,
                   ST_Y(delivery_location::geometry) as d_lat, ST_X(delivery_location::geometry) as d_lng
         FROM quotes WHERE id = $1`, [qId]
    );
    if (quoteRes.rows.length === 0) throw new Error(`Quote ${qId} not found`);
    const q = quoteRes.rows[0];

    // 3. Calculate Restricted Discount (Promo only applies to Delivery Fee)
    let deliveryFee = parseFloat(m.delivery_fee || 0);
    const itemPrice = parseFloat(m.item_price || 0);
    const platformFee = (m.fee_payer === 'PAYER') ? parseFloat(m.platform_fee_amount || 0) : 0;

    let discount = 0;
    if (m.promo_id) {
        try {
            const couponRes = await client.query("SELECT * FROM coupons WHERE id = $1 AND is_active = true", [m.promo_id]);
            if (couponRes.rows.length > 0) {
                const c = couponRes.rows[0];
                const calculatedDiscount = c.discount_type === 'FIXED' ? parseFloat(c.discount_value) : deliveryFee * (parseFloat(c.discount_value) / 100);

                // Rule: Promo only discounts delivery fee, never item price or platform fee.
                discount = Math.min(calculatedDiscount, deliveryFee);
                deliveryFee = Math.max(0, deliveryFee - discount);

                console.log(`[Activation] Applied Promo: ${c.code}. Discount: ${discount}. New Delivery Fee: ${deliveryFee}`);
            }
        } catch (e) { console.error('[Activation] Promo check failed:', e.message); }
    }

    const finalTotal = itemPrice + deliveryFee + platformFee;

    // 4. Insert Order
    const pCode = Math.floor(1000 + Math.random() * 9000).toString();
    const dCode = Math.floor(1000 + Math.random() * 9000).toString();
    const pHash = await bcrypt.hash(pCode, 10);
    const dHash = await bcrypt.hash(dCode, 10);

    const orderRes = await client.query(
        `INSERT INTO orders (
            order_type, user_id, quote_id, status, item_description, size_tier,
            pickup_address, delivery_address, pickup_location, delivery_location,
            total_fare, payment_status, payment_method, payment_reference, payment_channel,
            recipient_name, recipient_phone,
            pickup_display_summary, delivery_display_summary, item_price, delivery_fee,
            platform_fee_amount, fee_payer, initiator_role, escrow_status, seller_phone, payer_id,
            original_delivery_fee, original_total_fare, pickup_code_hash, delivery_code_hash,
            pickup_code, delivery_code, coupon_id, pickup_state
        ) VALUES (
            'pickup_delivery', $1, $2, $3, $4, $5, $6, $7,
            ST_SetSRID(ST_MakePoint($8, $9), 4326)::geography,
            ST_SetSRID(ST_MakePoint($10, $11), 4326)::geography,
            $12, $13, $14, $15, $16,
            $17, $18, $19, $20, $21, $22,
            $23, $24, $25, $26, $27, $28,
            $29, $30, $31, $32,
            $33, $34, $35::uuid, $36
        ) RETURNING id`,
        [
            m.user_id, // $1
            q.id,      // $2
            'PAYMENT_CAPTURED', // $3
            q.item_description, // $4
            q.size_tier, // $5
            q.pickup_address, // $6
            q.delivery_address, // $7
            pLng, // $8
            pLat, // $9
            dLng, // $10
            dLat, // $11
            finalTotal, // $12 (Final Total)
            'PAID', // $13
            channel, // $14 (payment_method)
            reference, // $15
            channel, // $16 (payment_channel)
            m.recipient_name || 'Recipient', // $17
            m.recipient_phone || '000', // $18
            q.pickup_address.substring(0, 50), // $19
            q.delivery_address.substring(0, 50), // $20
            itemPrice, // $21
            deliveryFee, // $22 (Discounted Delivery Fee)
            platformFee, // $23
            m.fee_payer || 'PAYER', // $24
            m.initiator_role || 'PAYER', // $25
            (itemPrice > 0) ? 'held' : 'not_applicable', // $26
            m.seller_phone || null, // $27
            m.payer_id || null, // $28
            parseFloat(q.delivery_fee), // $29
            parseFloat(q.total_fare), // $30
            pHash, // $31
            dHash, // $32
            pCode, // $33
            dCode, // $34
            m.promo_id || null, // $35
            m.pickup_state || q.pickup_state // $36
        ]
    );

    const orderId = orderRes.rows[0].id;

    // 5. Ledger & Notifications (Non-blocking)
    if (parseFloat(m.item_price || 0) > 0) {
        try {
            const sellerRes = await client.query("SELECT id FROM users WHERE phone = $1", [m.seller_phone]);
            if (sellerRes.rows.length > 0) {
                const sId = sellerRes.rows[0].id;
                await client.query("UPDATE orders SET seller_id = $1 WHERE id = $2", [sId, orderId]);
                const walletId = await walletService.ensureWalletExists(client, 'USER', sId);
                await walletService.recordEntry(client, walletId, 'CREDIT', parseFloat(m.item_price), 'ESCROW_HOLD', `Order #${orderId}`, orderId, 'pending');
            }
        } catch (e) { console.error('[Activation] Ledger Warning:', e.message); }
    }

    try {
        const uRes = await client.query("SELECT email, full_name FROM users WHERE id = $1", [m.user_id]);
        if (uRes.rows.length > 0) emailService.sendPaymentReceiptEmail(uRes.rows[0].email, uRes.rows[0].full_name, orderId, q.total_fare, q.item_description).catch(() => {});
    } catch (e) {}

    return orderId;
};

/**
 * Standard Webhook Handler.
 */
const handleWebhook = async (req, res) => {
  const secret = PAYSTACK_SECRET;
  const payload = req.rawBody || JSON.stringify(req.body);
  const hash = crypto.createHmac('sha512', secret).update(payload).digest('hex');

  if (hash !== req.headers['x-paystack-signature']) return res.sendStatus(401);

  const event = req.body;
  const data = event.data;

  if (event.event === 'charge.success') {
    const { reference, metadata, channel } = data;
    const m = typeof metadata === 'string' ? JSON.parse(metadata) : metadata;

    if (m?.collection_type === 'COD') {
        await db.query("UPDATE orders SET collection_status = 'collected', payment_status = 'PAID' WHERE id = $1", [m.order_id]);
        await walletService.processCoDRemittance(m.order_id).catch(() => {});
        return res.sendStatus(200);
    }

    if (m?.type === 'TOPUP') {
        const client = await db.pool.connect();
        try {
            await client.query('BEGIN');
            const walletId = await walletService.ensureWalletExists(client, 'USER', m.user_id);
            await walletService.recordEntry(client, walletId, 'CREDIT', data.amount/100, 'TOPUP', `Top-up Ref: ${reference}`);
            await client.query('COMMIT');
        } catch (e) { await client.query('ROLLBACK'); } finally { client.release(); }
        return res.sendStatus(200);
    }

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        const existing = await client.query("SELECT id FROM orders WHERE payment_reference = $1", [reference]);
        if (existing.rows.length === 0) await activatePaidMission(client, m, reference, channel);
        await client.query('COMMIT');
    } catch (e) { await client.query('ROLLBACK'); console.error('❌ Activation FAILED:', e.message); } finally { client.release(); }
  }

  res.sendStatus(200);
};

/**
 * Branded Redirect Page to return user to App.
 */
const handleWebhookGET = (req, res) => {
    const reference = req.query.reference || req.query.trxref;
    res.send(`
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Payment Success | Pikop</title>
            <style>
                body { font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; background: #F3F4F6; margin: 0; }
                .card { background: white; padding: 40px; border-radius: 24px; text-align: center; box-shadow: 0 10px 25px rgba(0,0,0,0.1); width: 90%; max-width: 400px; }
                .btn { display: block; background: #008751; color: white; padding: 16px; border-radius: 12px; text-decoration: none; font-weight: bold; margin-top: 24px; }
            </style>
        </head>
        <body>
            <div class="card">
                <div style="font-size: 50px; color: #008751;">✓</div>
                <h2>Mission Activated!</h2>
                <p>Your payment was successful. Return to the app to track your delivery.</p>
                <a href="pikop://payment/success?reference=${reference}" class="btn">RETURN TO PIKOP APP</a>
            </div>
            <script>
                setTimeout(() => { window.location.replace("pikop://payment/success?reference=${reference}"); }, 1000);
            </script>
        </body>
        </html>
    `);
};

/**
 * Direct Reference Verification (Android Client Fallback).
 */
const verifyPayment = async (req, res) => {
    const { reference } = req.params;
    try {
        const response = await axios.get(`https://api.paystack.co/transaction/verify/${reference}`, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
        });
        const tx = response.data.data;
        if (tx.status === 'success') {
            const client = await db.pool.connect();
            try {
                await client.query('BEGIN');
                const existing = await client.query("SELECT id FROM orders WHERE payment_reference = $1", [reference]);
                if (existing.rows.length === 0) await activatePaidMission(client, tx.metadata, reference, tx.channel);
                await client.query('COMMIT');
            } catch (e) { await client.query('ROLLBACK'); } finally { client.release(); }
            return res.status(200).json({ success: true, status: 'PAID' });
        }
        res.status(200).json({ success: false, status: tx.status });
    } catch (e) { res.status(500).json({ success: false, message: 'Verify failed' }); }
};

module.exports = { initializePayment, initializeCoDPayment, handleWebhook, handleWebhookGET, verifyPayment };
