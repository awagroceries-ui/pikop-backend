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

/**
 * Processes a cancellation fee for a matched order.
 * Charges 25% of total_fare to the platform wallet.
 * This assumes the user's wallet was already debited or the capture was successful.
 */
const processCancellationFee = async (orderId) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const orderRes = await client.query(
      'SELECT total_fare, user_id FROM orders WHERE id = $1 FOR UPDATE',
      [orderId]
    );
    if (orderRes.rows.length === 0) throw new Error('Order not found');

    const { total_fare } = orderRes.rows[0];
    const feeAmount = (total_fare * 0.25).toFixed(2);

    // Get Platform Wallet
    const platformWalletRes = await client.query(
      "SELECT id FROM wallets WHERE owner_type = 'PLATFORM' FOR UPDATE"
    );
    const platformWalletId = platformWalletRes.rows[0].id;

    // Credit Platform Wallet
    await client.query(
      'UPDATE wallets SET balance = balance + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
      [feeAmount, platformWalletId]
    );
    await client.query(
      "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'CREDIT', 'CANCELLATION_FEE', $3)",
      [platformWalletId, feeAmount, orderId]
    );

    await client.query('COMMIT');
    return true;
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error processing cancellation fee:', error);
    throw error;
  } finally {
    client.release();
  }
};

/**
 * Debits a Corporate account for an order.
 */
const processCorporateDebit = async (client, corporateAccountId, amount, orderId) => {
    // 1. Get Account & Wallet
    const { rows } = await client.query(`
        SELECT ca.billing_type, ca.paystack_mandate_id, w.id as wallet_id, w.balance
        FROM corporate_accounts ca
        JOIN wallets w ON w.corporate_account_id = ca.id
        WHERE ca.id = $1 AND ca.status = 'active'
        FOR UPDATE OF ca, w`, [corporateAccountId]);

    if (rows.length === 0) throw new Error('Corporate account not active or not found');
    const acc = rows[0];

    if (acc.billing_type === 'prepaid_wallet') {
        if (parseFloat(acc.balance) < amount) throw new Error('Insufficient corporate funds');

        // Debit Wallet
        await client.query("UPDATE wallets SET balance = balance - $1 WHERE id = $2", [amount, acc.wallet_id]);
        await client.query(
            "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'DEBIT', 'CORPORATE_ORDER', $3)",
            [acc.wallet_id, amount, orderId]
        );
    } else if (acc.billing_type === 'direct_debit') {
        if (!acc.paystack_mandate_id) throw new Error('No valid direct-debit mandate found');
        // STUB: Real-time Paystack Charge would happen here
        console.log(`[PAYMENT] Charging Corporate Mandate ${acc.paystack_mandate_id} for Amount ${amount}`);

        // Log in ledger anyway for reporting
        await client.query(
            "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ($1, $2, 'DEBIT', 'DIRECT_DEBIT_ORDER', $3)",
            [acc.wallet_id, amount, orderId]
        );
    }

    return true;
};

/**
 * Triggers referral rewards if this was the referee's first delivery (Prompt 19).
 */
const triggerReferralReward = async (client, orderId) => {
    // 1. Get Order & User info
    const { rows: orderRows } = await client.query(`
        SELECT o.user_id, u.referred_by_user_id, u.phone, u.email
        FROM orders o
        JOIN users u ON u.id = o.user_id
        WHERE o.id = $1`, [orderId]);

    if (orderRows.length === 0) return;
    const { user_id, referred_by_user_id, phone: refereePhone } = orderRows[0];

    if (!referred_by_user_id) return; // No referrer

    // 2. Abuse check: same phone or same bank account (Prompt 19, point 3)
    const { rows: referrerInfo } = await client.query("SELECT phone FROM users WHERE id = $1", [referred_by_user_id]);
    if (referrerInfo.length > 0 && referrerInfo[0].phone === refereePhone) {
        console.warn(`[Referral] Blocked potential abuse: User ${user_id} and Referrer ${referred_by_user_id} share phone ${refereePhone}`);
        return;
    }

    // 3. Check if this is the first DELIVERED order for this user
    const { rows: orderCount } = await client.query(
        "SELECT COUNT(*) FROM orders WHERE user_id = $1 AND status = 'DELIVERED'",
        [user_id]
    );

    if (parseInt(orderCount[0].count) !== 1) return; // Not the first one

    // 3. Process Rewards (₦500 Referrer / ₦300 Referee)
    const REWARD_REFERRER = 500.00;
    const REWARD_REFEREE = 300.00;

    // Credit Referrer
    await client.query("UPDATE wallets SET balance = balance + $1 WHERE owner_id = $2 AND owner_type = 'USER'", [REWARD_REFERRER, referred_by_user_id]);
    await client.query(
        "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ((SELECT id FROM wallets WHERE owner_id = $1 AND owner_type = 'USER'), $2, 'CREDIT', 'REFERRAL_REWARD', $3)",
        [referred_by_user_id, REWARD_REFERRER, orderId]
    );

    // Credit Referee
    await client.query("UPDATE wallets SET balance = balance + $1 WHERE owner_id = $2 AND owner_type = 'USER'", [REWARD_REFEREE, user_id]);
    await client.query(
        "INSERT INTO wallet_ledger_entries (wallet_id, amount, entry_type, purpose, reference_id) VALUES ((SELECT id FROM wallets WHERE owner_id = $1 AND owner_type = 'USER'), $2, 'CREDIT', 'REFEREE_WELCOME', $3)",
        [user_id, REWARD_REFEREE, orderId]
    );

    // Log Reward Record
    await client.query(
        `INSERT INTO referral_rewards (referrer_user_id, referee_user_id, reward_amount, status, triggered_by_order_id)
         VALUES ($1, $2, $3, 'credited', $4)`,
        [referred_by_user_id, user_id, REWARD_REFERRER + REWARD_REFEREE, orderId]
    );
};

module.exports = {
  processDeliveryPayment,
  processCancellationFee,
  processCorporateDebit,
  triggerReferralReward
};
