const db = require('../config/db');

/**
 * Processes payment for a completed delivery.
 * Splits 75% to fulfiller and 25% to platform.
 */
const processDeliveryPayment = async (orderId) => {
  const client = await db.pool.connect();

  try {
    await client.query('BEGIN');

    // 1. Get order details
    const orderRes = await client.query(
      'SELECT total_fare, fulfiller_id FROM orders WHERE id = $1 FOR UPDATE',
      [orderId]
    );
    if (orderRes.rows.length === 0) throw new Error('Order not found');

    const { total_fare, fulfiller_id } = orderRes.rows[0];
    const fulfillerShare = (total_fare * 0.75).toFixed(2);
    const platformShare = (total_fare * 0.25).toFixed(2);

    // 2. Get/Create Fulfiller Wallet
    let fulfillerWalletRes = await client.query(
      "SELECT id FROM wallets WHERE owner_id = $1 AND owner_type = 'FULFILLER' FOR UPDATE",
      [fulfiller_id]
    );

    let fulfillerWalletId;
    if (fulfillerWalletRes.rows.length === 0) {
      const newWallet = await client.query(
        "INSERT INTO wallets (owner_id, owner_type, balance) VALUES ($1, 'FULFILLER', 0) RETURNING id",
        [fulfiller_id]
      );
      fulfillerWalletId = newWallet.rows[0].id;
    } else {
      fulfillerWalletId = fulfillerWalletRes.rows[0].id;
    }

    // 3. Get Platform Wallet
    const platformWalletRes = await client.query(
      "SELECT id FROM wallets WHERE owner_type = 'PLATFORM' FOR UPDATE"
    );
    const platformWalletId = platformWalletRes.rows[0].id;

    // 4. Update Fulfiller Wallet & Ledger
    await client.query(
      'UPDATE wallets SET balance = balance + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
      [fulfillerShare, fulfillerWalletId]
    );
    await client.query(
      "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'CREDIT', 'DELIVERY_PAYMENT', $3)",
      [fulfillerWalletId, fulfillerShare, orderId]
    );

    // 5. Update Platform Wallet & Ledger
    await client.query(
      'UPDATE wallets SET balance = balance + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
      [platformShare, platformWalletId]
    );
    await client.query(
      "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'CREDIT', 'COMMISSION', $3)",
      [platformWalletId, platformShare, orderId]
    );

    await client.query('COMMIT');
    return true;
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error processing delivery payment:', error);
    throw error;
  } finally {
    client.release();
  }
};

module.exports = {
  processDeliveryPayment
};
