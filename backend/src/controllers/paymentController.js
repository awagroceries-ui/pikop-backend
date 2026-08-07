const crypto = require('crypto');
const db = require('../config/db');
require('dotenv').config();

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
  const { reference, customer } = data;
  // Use metadata or reference to link back to our quote/order if needed
  // For alpha, we assume the Android app calls createOrder after success,
  // but we could automate it here for extra robustness.
  console.log(`Payment successful for ${customer.email}. Ref: ${reference}`);
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
