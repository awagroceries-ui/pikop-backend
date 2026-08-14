const crypto = require('crypto');
const db = require('../config/db');
const axios = require('axios');
require('dotenv').config();

/**
 * Initializes a Paystack transaction to get an authorization URL.
 */
const initializePayment = async (req, res) => {
    const { amount, email, metadata } = req.body;
    const secret = process.env.PAYSTACK_SECRET_KEY;

    try {
        const response = await axios.post(
            'https://api.paystack.co/transaction/initialize',
            {
                amount: Math.round(amount), // Already in Kobo
                email,
                metadata,
                // Do NOT restrict channels to allow user selection (Prompt 9)
                channels: ['card', 'bank', 'ussd', 'qr', 'mobile_money', 'bank_transfer']
            },
            {
                headers: {
                    Authorization: `Bearer ${secret}`,
                    'Content-Type': 'application/json'
                }
            }
        );

        res.status(200).json(response.data.data);
    } catch (error) {
        console.error('Paystack Initialize Error:', error.response?.data || error.message);
        res.status(500).json({ error: 'Failed to initialize payment' });
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
  const { reference, customer, channel } = data;

  // Update Order with specific Paystack channel (ussd, bank, etc.)
  await db.query(
      "UPDATE orders SET payment_method = $1 WHERE id = (SELECT reference_id FROM wallet_ledger_entries WHERE reference_id::text = $2 LIMIT 1)",
      [channel, reference]
  );

  console.log(`Payment successful via ${channel} for ${customer.email}. Ref: ${reference}`);
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
  handleWebhook
};
