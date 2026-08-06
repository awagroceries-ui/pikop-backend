const axios = require('axios');
require('dotenv').config();

const PAYSTACK_SECRET_KEY = process.env.PAYSTACK_SECRET_KEY;

/**
 * Initiates a transfer via Paystack.
 * @param {number} amount - Amount in Naira (will be converted to Kobo).
 * @param {string} recipientCode - The recipient code from Paystack.
 * @param {string} reference - Unique reference for the transaction.
 */
const initiateTransfer = async (amount, recipientCode, reference) => {
  try {
    const response = await axios.post(
      'https://api.paystack.co/transfer',
      {
        source: 'balance',
        amount: Math.round(amount * 100), // Convert to Kobo
        recipient: recipientCode,
        reference: reference,
        reason: 'Pikop Fulfiller Payout'
      },
      {
        headers: {
          Authorization: `Bearer ${PAYSTACK_SECRET_KEY}`,
          'Content-Type': 'application/json'
        }
      }
    );

    return response.data;
  } catch (error) {
    console.error('Paystack Transfer Error:', error.response?.data || error.message);
    throw new Error('Failed to initiate Paystack transfer');
  }
};

module.exports = {
  initiateTransfer
};
