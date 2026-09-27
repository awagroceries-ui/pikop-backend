const db = require('../config/db');
const axios = require('axios');
require('dotenv').config();

/**
 * Gets the authenticated user's wallet information and recent transactions.
 */
const getWalletInfo = async (req, res) => {
  const userId = req.user.id;

  try {
    // 1. Check if user is a fulfiller (to determine ownerType)
    const fulfillerRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    const isFulfiller = fulfillerRes.rows.length > 0;
    const ownerType = isFulfiller ? 'FULFILLER' : 'USER';

    // 2. Get Wallet Balance
    const walletRes = await db.query(
      "SELECT id, balance, currency FROM wallets WHERE owner_id = $1 AND owner_type = $2",
      [userId, ownerType]
    );

    if (walletRes.rows.length === 0) {
      return res.status(200).json({ balance: 0, currency: 'NGN', transactions: [] });
    }

    const wallet = walletRes.rows[0];

    // 2. Get Recent Transactions (Ledger Entries)
    const ledgerRes = await db.query(
      `SELECT id, amount, entry_type, purpose, reference_id, created_at
       FROM wallet_ledger_entries
       WHERE wallet_id = $1
       ORDER BY created_at DESC
       LIMIT 20`,
      [wallet.id]
    );

    res.status(200).json({
      balance: parseFloat(wallet.balance),
      currency: wallet.currency,
      transactions: ledgerRes.rows
    });
  } catch (error) {
    console.error('Get Wallet Info Error:', error);
    res.status(500).json({ error: 'Failed to fetch wallet information' });
  }
};

/**
 * Initializes a wallet topup via Paystack.
 */
const initializeTopup = async (req, res) => {
  const userId = req.user.id;
  const email = req.user.email;
  const { amount } = req.body;

  if (!amount || amount <= 0) {
    return res.status(400).json({ error: 'Valid amount is required' });
  }

  const secret = process.env.PAYSTACK_SECRET_KEY;
  if (!secret || secret.includes('your_')) {
    console.error('[Paystack] ERROR: Secret Key is missing or invalid in .env');
    return res.status(500).json({ error: 'Payment system not configured on server' });
  }

  try {
    const amountInKobo = Math.round(amount * 100);
    const response = await axios.post(
      'https://api.paystack.co/transaction/initialize',
      {
        amount: amountInKobo,
        email,
        metadata: {
          type: 'wallet_topup',
          userId
        },
        callback_url: 'pikop://payment/success'
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
    console.error('Initialize Topup Error:', error.response?.data || error.message);
    res.status(500).json({ error: 'Failed to initialize topup', detail: error.response?.data?.message || error.message });
  }
};

module.exports = {
  getWalletInfo,
  initializeTopup
};
