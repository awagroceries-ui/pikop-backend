const crypto = require('crypto');
const db = require('../config/db');
const axios = require('axios');
require('dotenv').config();

/**
 * Helper to credit a user's wallet for a successful topup (Idempotent).
 */
const creditWalletFromTopup = async (userId, amount, reference) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Idempotency check via reference_id
    const existingLedger = await client.query(
      "SELECT id FROM wallet_ledger_entries WHERE reference_id = $1 LIMIT 1",
      [reference]
    );

    if (existingLedger.rows.length > 0) {
      await client.query('COMMIT');
      console.log(`[Topup] Reference ${reference} already credited.`);
      return true;
    }

    // 2. Check if user is a fulfiller to determine ownerType
    const fulfillerRes = await client.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    const isFulfiller = fulfillerRes.rows.length > 0;
    const ownerType = isFulfiller ? 'FULFILLER' : 'USER';

    // 3. Get or Create Wallet
    let walletRes = await client.query(
      "SELECT id FROM wallets WHERE owner_id = $1 AND owner_type = $2 FOR UPDATE",
      [userId, ownerType]
    );

    let walletId;
    if (walletRes.rows.length === 0) {
      const newWallet = await client.query(
        "INSERT INTO wallets (owner_id, owner_type, balance) VALUES ($1, $2, 0) RETURNING id",
        [userId, ownerType]
      );
      walletId = newWallet.rows[0].id;
    } else {
      walletId = walletRes.rows[0].id;
    }

    // 4. Update Balance & Add Ledger Record
    await client.query(
      "UPDATE wallets SET balance = balance + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2",
      [amount, walletId]
    );

    await client.query(
      "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'CREDIT', 'TOPUP', $3)",
      [walletId, amount, reference]
    );

    await client.query('COMMIT');
    console.log(`[Topup] Successfully credited User ${userId} with ₦${amount}. Ref: ${reference}`);
    return true;
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('[Topup] Credit Wallet Error:', error);
    throw error;
  } finally {
    client.release();
  }
};

/**
 * Initializes a Paystack transaction to get an authorization URL.
 */
const initializePayment = async (req, res) => {
    const { amount, email, metadata } = req.body;
    const secret = process.env.PAYSTACK_SECRET_KEY;

    if (!secret || secret.includes('your_')) {
        console.error('[Paystack] ERROR: Secret Key is missing or invalid in .env');
        return res.status(500).json({ error: 'Payment system not configured on server' });
    }

    try {
        console.log(`[Paystack] Initializing transaction for ${email} - Amount: ${amount} kobo`);
        const response = await axios.post(
            'https://api.paystack.co/transaction/initialize',
            {
                amount: Math.round(amount), // Already in Kobo
                email,
                metadata,
                channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer']
            },
            {
                headers: {
                    Authorization: `Bearer ${secret.trim()}`,
                    'Content-Type': 'application/json'
                },
                timeout: 10000
            }
        );

        res.status(200).json(response.data.data);
    } catch (error) {
        console.error('Paystack Initialize Error:', error.response?.data || error.message);
        const detail = error.response?.data?.message || error.message;
        res.status(500).json({ error: 'Failed to initialize payment', detail });
    }
};

/**
 * Verifies a payment reference with Paystack.
 */
const verifyPayment = async (req, res) => {
    const { reference } = req.params;
    const secret = process.env.PAYSTACK_SECRET_KEY;

    if (!secret || secret.includes('your_')) {
        return res.status(500).json({ error: 'Payment system not configured on server' });
    }

    try {
        const response = await axios.get(
            `https://api.paystack.co/transaction/verify/${encodeURIComponent(reference)}`,
            {
                headers: {
                    Authorization: `Bearer ${secret.trim()}`
                },
                timeout: 10000
            }
        );

        const data = response.data?.data;
        if (data && data.status === 'success') {
            const metadata = data.metadata || {};
            if (metadata.type === 'wallet_topup' && metadata.userId) {
                const amountInNaira = data.amount / 100;
                await creditWalletFromTopup(metadata.userId, amountInNaira, reference);
                return res.status(200).json({
                    success: true,
                    type: 'wallet_topup',
                    amount: amountInNaira,
                    reference
                });
            }

            // General order payment
            return res.status(200).json({
                success: true,
                order_id: metadata.orderId || metadata.order_id,
                reference
            });
        }

        res.status(400).json({ success: false, status: data?.status || 'failed' });
    } catch (error) {
        console.error('Verify Payment Error:', error.response?.data || error.message);
        res.status(500).json({ error: 'Verification failed', detail: error.message });
    }
};

/**
 * Handles incoming webhooks from Paystack.
 */
const handleWebhook = async (req, res) => {
  const secret = process.env.PAYSTACK_SECRET_KEY;
  const hash = crypto.createHmac('sha512', secret).update(JSON.stringify(req.body)).digest('hex');

  // Verify signature
  if (hash !== req.headers['x-paystack-signature']) {
    return res.status(400).send('Invalid signature');
  }

  const event = req.body;
  console.log('Received Paystack Event:', event.event);

  try {
    switch (event.event) {
      case 'charge.success':
        await handleChargeSuccess(event.data);
        break;
      case 'transfer.success':
        await handleTransferStatus(event.data, 'SUCCESSFUL');
        break;
      case 'transfer.failed':
        await handleTransferStatus(event.data, 'FAILED');
        break;
      default:
        console.log('Unhandled event type:', event.event);
    }
  } catch (error) {
    console.error('Webhook Error:', error.message);
  }

  res.status(200).send('OK');
};

const handleChargeSuccess = async (data) => {
  const { reference, customer, channel, metadata, amount } = data;

  if (metadata?.type === 'wallet_topup' && metadata?.userId) {
    const amountInNaira = amount / 100;
    await creditWalletFromTopup(metadata.userId, amountInNaira, reference);
    console.log(`Wallet topup processed via webhook for user ${metadata.userId}. Ref: ${reference}`);
    return;
  }

  // Update Order with specific Paystack channel (ussd, bank, etc.)
  await db.query(
      "UPDATE orders SET payment_method = $1 WHERE id = (SELECT reference_id FROM wallet_ledger_entries WHERE reference_id::text = $2 LIMIT 1)",
      [channel, reference]
  );

  console.log(`Payment successful via ${channel} for ${customer?.email}. Ref: ${reference}`);
};

const handleTransferStatus = async (data, status) => {
  const { reference, transfer_code } = data;
  await db.query(
    "UPDATE withdrawals SET status = $1, paystack_transfer_code = $2 WHERE paystack_reference = $3",
    [status, transfer_code, reference]
  );
  console.log(`Withdrawal ${reference} updated to ${status}`);
};

module.exports = {
  initializePayment,
  verifyPayment,
  handleWebhook
};
