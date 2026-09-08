const db = require('../config/db');
const walletService = require('../services/walletService');

const paystackService = require('../services/paystackService');
const axios = require('axios');
const PAYSTACK_SECRET = (process.env.PAYSTACK_SECRET_KEY || '').trim();

/**
 * Fetches user wallet balance and history.
 */
const getMyWallet = async (req, res) => {
  const userId = req.user.id;
  const userRole = req.user.role;

  try {
    // 1. Determine Owner Type/ID
    let ownerType = 'USER';
    let ownerId = userId;

    if (userRole === 'FULFILLER') {
        const fRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
        if (fRes.rows.length > 0) {
            ownerType = 'FULFILLER';
            ownerId = fRes.rows[0].id;
        }
    }

    // 2. Fetch Wallet
    const { rows: wallets } = await db.query(
        "SELECT id, balance, pending_balance, currency FROM wallets WHERE owner_type = $1 AND owner_id = $2",
        [ownerType, ownerId.toString()]
    );

    if (wallets.length === 0) {
        return res.status(200).json({ success: true, data: { balance: 0, pending_balance: 0, currency: 'NGN', transactions: [] } });
    }

    const wallet = wallets[0];

    // 3. Fetch History
    const { rows: history } = await db.query(
        "SELECT * FROM wallet_ledger_entries WHERE wallet_id = $1 ORDER BY created_at DESC LIMIT 50",
        [wallet.id]
    );

    res.status(200).json({
        success: true,
        data: {
            balance: parseFloat(wallet.balance),
            pending_balance: parseFloat(wallet.pending_balance || 0),
            currency: wallet.currency,
            transactions: history
        }
    });

  } catch (error) {
    throw error;
  }
};

/**
 * Requests a withdrawal (Fulfillers only).
 */
const requestWithdrawal = async (req, res) => {
    const { amount } = req.body;
    const userId = req.user.id;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Auth Check
        const fRes = await client.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
        if (fRes.rows.length === 0) return res.status(403).json({ success: false, message: 'Only fulfillers can withdraw' });
        const fulfillerId = fRes.rows[0].id;

        // 2. Wallet Check
        const walletId = await walletService.ensureWalletExists(client, 'FULFILLER', fulfillerId);
        const { rows } = await client.query("SELECT balance FROM wallets WHERE id = $1 FOR UPDATE", [walletId]);

        if (parseFloat(rows[0].balance) < parseFloat(amount)) {
            return res.status(400).json({ success: false, message: 'Insufficient balance' });
        }

        // 3. Debit Wallet
        await walletService.recordEntry(client, walletId, 'DEBIT', amount, 'WITHDRAWAL', 'Payout requested');

        // 4. Create Withdrawal Record
        await client.query(
            "INSERT INTO withdrawals (fulfiller_id, wallet_id, amount, status) VALUES ($1, $2, $3, 'PENDING')",
            [fulfillerId, walletId, amount]
        );

        await client.query('COMMIT');
        res.status(201).json({ success: true, message: 'Withdrawal request submitted' });

    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
};

/**
 * Initializes a Paystack transaction for Wallet Top-up.
 */
const initializeTopup = async (req, res) => {
    const { amount } = req.body;
    const userId = req.user.id;
    const email = req.user.email;

    try {
        const koboAmount = Math.round(parseFloat(amount) * 100);
        if (koboAmount < 100) return res.status(400).json({ success: false, message: 'Minimum top-up is ₦1.00' });

        const payload = {
            amount: koboAmount,
            email,
            currency: 'NGN',
            callback_url: 'pikop://wallet/topup/success',
            channels: ['card', 'bank', 'ussd', 'bank_transfer', 'qr', 'mobile_money'],
            metadata: {
                user_id: userId,
                type: 'TOPUP'
            }
        };

        const response = await axios.post('https://api.paystack.co/transaction/initialize', payload, {
            headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
        });

        res.status(200).json(response.data.data);
    } catch (error) {
        console.error('[Wallet] Top-up Init Error:', error.message);
        res.status(400).json({ success: false, message: 'Failed to initialize top-up' });
    }
};

module.exports = {
  getMyWallet,
  requestWithdrawal,
  initializeTopup
};
