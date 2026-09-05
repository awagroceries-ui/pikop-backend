const axios = require('axios');
require('dotenv').config();

const PAYSTACK_SECRET_KEY = (process.env.PAYSTACK_SECRET_KEY || '').trim();

/**
 * Creates a transfer recipient on Paystack.
 */
const createTransferRecipient = async (name, accountNumber, bankCode) => {
  try {
    const response = await axios.post(
      'https://api.paystack.co/transferrecipient',
      {
        type: 'nuban',
        name: name,
        account_number: accountNumber,
        bank_code: bankCode,
        currency: 'NGN'
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
    console.error('[PaystackService] Create Recipient Error:', error.response?.data || error.message);
    throw new Error(error.response?.data?.message || 'Failed to create transfer recipient');
  }
};

/**
 * Initiates a transfer via Paystack.
 */
const initiateTransfer = async (amount, recipientCode, reference) => {
  try {
    const response = await axios.post(
      'https://api.paystack.co/transfer',
      {
        source: 'balance',
        amount: Math.round(parseFloat(amount) * 100), // Convert to Kobo
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
    console.error('[PaystackService] Transfer Error:', error.response?.data || error.message);
    throw new Error(error.response?.data?.message || 'Failed to initiate transfer');
  }
};

/**
 * Fetches the list of Nigerian banks from Paystack.
 */
const getBanks = async () => {
  try {
    const response = await axios.get('https://api.paystack.co/bank', {
      headers: { Authorization: `Bearer ${PAYSTACK_SECRET_KEY}` }
    });
    return response.data;
  } catch (error) {
    console.error('[PaystackService] Get Banks Error:', error.response?.data || error.message);
    throw new Error('Failed to fetch bank list');
  }
};

/**
 * Resolves account number to account name.
 */
const resolveAccount = async (accountNumber, bankCode) => {
  try {
    const response = await axios.get(`https://api.paystack.co/bank/resolve?account_number=${accountNumber}&bank_code=${bankCode}`, {
      headers: { Authorization: `Bearer ${PAYSTACK_SECRET_KEY}` }
    });
    return response.data;
  } catch (error) {
    console.error('[PaystackService] Resolve Account Error:', error.response?.data || error.message);
    throw new Error(error.response?.data?.message || 'Account resolution failed');
  }
};

module.exports = {
  createTransferRecipient,
  initiateTransfer,
  getBanks,
  resolveAccount
};
