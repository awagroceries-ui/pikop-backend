const db = require('../config/db');
const paystackService = require('../services/paystackService');
const { v4: uuidv4 } = require('uuid');

/**
 * Handles withdrawal requests from fulfillers.
 */
const requestWithdrawal = async (req, res) => {
  const { amount, type } = req.body; // type: 'STANDARD' or 'INSTANT'
  const fulfillerId = req.user?.fulfillerId || req.body.fulfillerId; // Get from JWT in prod

  const client = await db.pool.connect();

  try {
    await client.query('BEGIN');

    // 1. Get Wallet & Fulfiller Details
    const walletQuery = `
      SELECT w.id as wallet_id, w.balance, f.paystack_recipient_code
      FROM wallets w
      JOIN fulfillers f ON f.id = w.owner_id
      WHERE f.id = $1 AND w.owner_type = 'FULFILLER'
      FOR UPDATE
    `;
    const { rows } = await client.query(walletQuery, [fulfillerId]);

    if (rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Wallet not found' });
    }

    const { wallet_id, balance, paystack_recipient_code } = rows[0];

    if (parseFloat(balance) < parseFloat(amount)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Insufficient balance' });
    }

    // 2. Deduct from Wallet & Create Ledger Entry
    await client.query(
      "UPDATE wallets SET balance = balance - $1 WHERE id = $2",
      [amount, wallet_id]
    );

    const withdrawalRef = `wd-${Date.now()}`;
    const { rows: withdrawalRows } = await client.query(
      "INSERT INTO withdrawals (fulfiller_id, wallet_id, amount, status, paystack_reference) VALUES ($1, $2, $3, 'PENDING', $4) RETURNING id",
      [fulfillerId, wallet_id, amount, withdrawalRef]
    );
    const withdrawalId = withdrawalRows[0].id;

    await client.query(
      "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'DEBIT', 'WITHDRAWAL', $3)",
      [wallet_id, amount, withdrawalId]
    );

    await client.query('COMMIT');

    // 3. Process Instant Withdrawal via Paystack
    if (type === 'INSTANT') {
      try {
        const transfer = await paystackService.initiateTransfer(amount, paystack_recipient_code, withdrawalRef);

        await db.query(
          "UPDATE withdrawals SET status = 'SUCCESSFUL', paystack_transfer_code = $1 WHERE id = $2",
          [transfer.data.transfer_code, withdrawalId]
        );

        return res.status(200).json({ message: 'Instant withdrawal successful', withdrawalId });
      } catch (paystackError) {
        // Log error, but withdrawal is already recorded as PENDING in DB for manual retry/ops
        console.error('Paystack Instant Transfer Failed:', paystackError.message);
        return res.status(200).json({
          message: 'Withdrawal requested, but instant processing failed. It will be processed manually.',
          withdrawalId
        });
      }
    }

    res.status(200).json({ message: 'Withdrawal request submitted', withdrawalId });

  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Withdrawal Request Error:', error);
    res.status(500).json({ error: 'Internal server error' });
  } finally {
    client.release();
  }
};

module.exports = {
  requestWithdrawal
};
