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

  try {
    // Standardize on a single USER wallet per human ID
    const walletId = await walletService.ensureWalletExists(db, 'USER', userId);

    const { rows: wallets } = await db.query(
        "SELECT id, balance, pending_balance, currency FROM wallets WHERE id = $1",
        [walletId]
    );

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
 * Includes "Instant Payout" logic via Paystack.
 */
const requestWithdrawal = async (req, res) => {
    const { amount, bank_name, account_number, bank_code, account_name } = req.body;
    const userId = req.user.id;

    // Policy: Minimum withdrawal amount
    const MIN_WITHDRAWAL = 1000;
    const INSTANT_THRESHOLD = 5000;

    if (parseFloat(amount || 0) < MIN_WITHDRAWAL) {
        return res.status(400).json({ success: false, message: `Minimum withdrawal is ₦${MIN_WITHDRAWAL}` });
    }

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Fetch user & bank details
        let bankName = bank_name;
        let accountNumber = account_number;
        let bankCode = bank_code;
        let accountName = account_name;
        let fulfillerId = null;

        const fRes = await client.query(
            "SELECT id, bank_name, account_number, bank_code, account_name, paystack_recipient_code FROM fulfillers WHERE user_id = $1",
            [userId]
        );

        if (fRes.rows.length > 0) {
            const f = fRes.rows[0];
            fulfillerId = f.id;
            bankName = bankName || f.bank_name;
            accountNumber = accountNumber || f.account_number;
            bankCode = bankCode || f.bank_code;
            accountName = accountName || f.account_name;
        } else {
            const uRes = await client.query("SELECT full_name FROM users WHERE id = $1", [userId]);
            accountName = accountName || (uRes.rows[0]?.full_name || 'Valued User');
        }

        if (!accountNumber || !bankCode) {
            await client.query('ROLLBACK');
            return res.status(400).json({ success: false, message: 'Please select a bank and enter a 10-digit account number.' });
        }

        // 2. Unified Wallet Check
        const walletId = await walletService.ensureWalletExists(client, 'USER', userId);
        const { rows: wRows } = await client.query("SELECT balance FROM wallets WHERE id = $1 FOR UPDATE", [walletId]);

        if (parseFloat(wRows[0]?.balance || 0) < parseFloat(amount)) {
            await client.query('ROLLBACK');
            return res.status(400).json({ success: false, message: 'Insufficient wallet balance' });
        }

        // 3. Ensure Paystack Recipient exists
        let recipientCode = null;
        try {
            const recipientRes = await paystackService.createTransferRecipient(accountName, accountNumber, bankCode);
            recipientCode = recipientRes.data.recipient_code;
        } catch (pErr) {
            console.warn('[Withdrawal] Paystack recipient creation warning:', pErr.message);
        }

        // 4. Record debit
        await walletService.recordEntry(client, walletId, 'DEBIT', amount, 'WITHDRAWAL', 'Payout requested');

        // 5. Instant Payout Logic
        let status = 'PENDING';
        let transferCode = null;

        if (recipientCode && parseFloat(amount) <= INSTANT_THRESHOLD) {
            try {
                const ref = `WDL_INST_${Date.now()}`;
                const transferRes = await paystackService.initiateTransfer(amount, recipientCode, ref);
                status = 'PROCESSING';
                transferCode = transferRes.data.transfer_code;
                console.log(`[Payout] Instant transfer initiated for User ${userId}: ${transferCode}`);
            } catch (pErr) {
                console.warn(`[Payout] Instant attempt failed, falling back to manual review:`, pErr.message);
            }
        }

        // 6. Create Withdrawal Record
        await client.query(
            "INSERT INTO withdrawals (fulfiller_id, wallet_id, amount, status, paystack_transfer_code) VALUES ($1, $2, $3, $4, $5)",
            [fulfillerId, walletId, amount, status, transferCode]
        );

        await client.query('COMMIT');

        res.status(201).json({
            success: true,
            message: status === 'PROCESSING' ? 'Payout initiated! Your funds are on the way.' : 'Withdrawal request submitted for review.'
        });

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Withdrawal] Request Failed:', error.message);
        res.status(500).json({ success: false, message: error.message });
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
