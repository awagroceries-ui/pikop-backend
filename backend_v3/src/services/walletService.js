const db = require('../config/db');

/**
 * Ensures a wallet exists for the given owner.
 */
const ensureWalletExists = async (client, ownerType, ownerId) => {
  const { rows } = await client.query(
    "SELECT id FROM wallets WHERE owner_type = $1 AND owner_id = $2",
    [ownerType, ownerId.toString()]
  );

  if (rows.length > 0) return rows[0].id;

  const createRes = await client.query(
    "INSERT INTO wallets (owner_type, owner_id, balance) VALUES ($1, $2, 0) RETURNING id",
    [ownerType, ownerId.toString()]
  );
  return createRes.rows[0].id;
};

/**
 * Records an immutable ledger entry and updates wallet balance.
 */
const recordEntry = async (client, walletId, type, amount, purpose, description, orderId = null, target = 'available', metadata = null) => {
  const numericAmount = parseFloat(amount);
  const balanceColumn = target === 'pending' ? 'pending_balance' : 'balance';

  // 1. Update specified balance
  const walletRes = await client.query(
    `UPDATE wallets SET ${balanceColumn} = ${balanceColumn} + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING balance, pending_balance`,
    [type === 'CREDIT' ? numericAmount : -numericAmount, walletId]
  );

  const resultingBalance = target === 'pending' ? walletRes.rows[0].pending_balance : walletRes.rows[0].balance;

  // 2. Record ledger
  await client.query(
    `INSERT INTO wallet_ledger_entries (wallet_id, order_id, entry_type, amount, balance_after, purpose, description, metadata)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
    [walletId, orderId, type, numericAmount, resultingBalance, purpose, description, metadata ? JSON.stringify(metadata) : null]
  );

  return resultingBalance;
};

/**
 * Processes mission settlement (75/25 Split).
 */
const processMissionSettlement = async (orderId, providedClient = null) => {
  const client = providedClient || await db.pool.connect();
  let shouldRelease = !providedClient;

  try {
    if (shouldRelease) await client.query('BEGIN');

    // 1. Fetch order details with User ID instead of just Fulfiller ID
    const orderRes = await client.query(
        `SELECT o.id, o.user_id, o.fulfiller_id, o.total_fare, o.delivery_fee, o.item_price,
                o.original_delivery_fee, o.fee_payer, o.platform_fee_amount, o.sms_charge_amount,
                f.user_id as fulfiller_user_id
         FROM orders o
         LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
         WHERE o.id = $1`,
        [orderId]
    );
    const order = orderRes.rows[0];

    if (!order) throw new Error(`Order #${orderId} not found for settlement`);

    if (!order.fulfiller_id || !order.fulfiller_user_id) {
        console.warn(`[Wallet] Settlement skipped for Order #${orderId}: No fulfiller or linked user found.`);
        if (shouldRelease) await client.query('COMMIT');
        return;
    }

    const settlableAmount = parseFloat(order.original_delivery_fee || order.delivery_fee || order.total_fare || 0);

    // 2. Fetch Split Config from Settings
    const settingsRes = await client.query("SELECT value FROM settings WHERE key = 'platform_commission'");
    const commissionRate = parseFloat(settingsRes.rows[0]?.value || '0.25');

    const platformShare = settlableAmount * commissionRate;
    const fulfillerShare = settlableAmount - platformShare;

    // 3. Fulfiller Credit (NOW UNIFIED TO USER WALLET)
    const fWalletId = await ensureWalletExists(client, 'USER', order.fulfiller_user_id);
    await recordEntry(client, fWalletId, 'CREDIT', fulfillerShare, 'SETTLEMENT', `Earnings for Mission #${order.id}`, order.id);

    // 4. Platform Credit (Delivery Share)
    const pWalletId = await ensureWalletExists(client, 'PLATFORM', 'SYSTEM');
    await recordEntry(client, pWalletId, 'CREDIT', platformShare, 'COMMISSION', `Commission for Mission #${order.id}`, order.id);

    // 5. Consolidated Platform Fee (Secure Pay - if paid by PAYER)
    if (order.fee_payer === 'PAYER' && parseFloat(order.platform_fee_amount) > 0) {
        await recordEntry(client, pWalletId, 'CREDIT', order.platform_fee_amount, 'SECURE_PAY_FEE', `Escrow service fee for Order #${order.id}`, order.id);
    }

    // 6. Guest SMS Charge (if applicable)
    if (parseFloat(order.sms_charge_amount) > 0) {
        await recordEntry(client, pWalletId, 'CREDIT', order.sms_charge_amount, 'SMS_CHARGE', `Guest notification fee for Order #${order.id}`, order.id);
    }

    if (shouldRelease) await client.query('COMMIT');
    console.log(`[Wallet] Settled Mission #${order.id}: Fulfiller +${fulfillerShare}, Platform +${platformShare + (order.sms_charge_amount || 0)}`);

    // 7. Trigger Growth Logic
    try {
        await awardLoyaltyPoints(client, order.user_id, settlableAmount);
        const orderCountRes = await client.query("SELECT COUNT(*) FROM orders WHERE user_id = $1 AND status IN ('DELIVERED', 'RELEASED')", [order.user_id]);
        if (parseInt(orderCountRes.rows[0].count) === 1) {
            await processReferralReward(client, order.user_id);
        }
    } catch (gErr) {
        console.error('[Growth] Trigger Error:', gErr.message);
    }
  } catch (error) {
    if (shouldRelease) await client.query('ROLLBACK');
    console.error('[Wallet] Settlement Failed:', error.message);
    throw error;
  } finally {
    if (shouldRelease) client.release();
  }
};

/**
 * Remits collected CoD funds to the Vendor.
 */
const processCoDRemittance = async (orderId, providedClient = null) => {
    const client = providedClient || await db.pool.connect();
    let shouldRelease = !providedClient;
    try {
        if (shouldRelease) await client.query('BEGIN');

        const { rows } = await client.query(
            "SELECT id, vendor_id, collect_on_delivery_amount FROM orders WHERE id = $1",
            [orderId]
        );
        const order = rows[0];
        if (!order.vendor_id) throw new Error('Order is not a vendor order');

        const vWalletId = await ensureWalletExists(client, 'VENDOR', order.vendor_id);
        await recordEntry(client, vWalletId, 'CREDIT', order.collect_on_delivery_amount, 'COD_COLLECTION', `Payment collected for Order #${order.id}`, order.id);

        if (shouldRelease) await client.query('COMMIT');
        console.log(`[Wallet] CoD Remitted to Vendor ${order.vendor_id} for Order ${order.id}`);
    } catch (error) {
        if (shouldRelease) await client.query('ROLLBACK');
        console.error('[Wallet] CoD Remittance Failed:', error.message);
    } finally {
        if (shouldRelease) client.release();
    }
};

/**
 * Moves funds from pending_balance to available_balance for the seller.
 */
const releaseEscrow = async (orderId, providedClient = null) => {
  const client = providedClient || await db.pool.connect();
  let shouldRelease = !providedClient;

  try {
    if (shouldRelease) await client.query('BEGIN');

    // 1. Fetch order details with fee info
    const orderRes = await client.query(
      `SELECT o.id, o.fulfiller_id, o.item_price, o.platform_fee_amount, o.fee_payer, o.user_id, o.seller_id,
              o.escrow_status, f.user_id as fulfiller_user_id
       FROM orders o
       LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
       WHERE o.id = $1 FOR UPDATE`,
      [orderId]
    );
    const order = orderRes.rows[0];

    if (!order || order.escrow_status !== 'held') {
      throw new Error('Order not eligible for escrow release');
    }

    const itemPrice = parseFloat(order.item_price);
    const fee = parseFloat(order.platform_fee_amount);

    // 2. Determine Seller Payout
    const sellerPayout = order.fee_payer === 'SELLER' ? (itemPrice - fee) : itemPrice;

    // 3. Update Seller Wallet (ALWAYS USER TYPE NOW)
    const targetUserId = order.seller_id || order.fulfiller_user_id || order.user_id;
    const sellerWalletId = await ensureWalletExists(client, 'USER', targetUserId);

    // Debit Pending
    await recordEntry(client, sellerWalletId, 'DEBIT', itemPrice, 'ESCROW_RELEASE', `Releasing escrow for Order #${order.id}`, order.id, 'pending');

    // Credit Available
    await recordEntry(client, sellerWalletId, 'CREDIT', sellerPayout, 'SETTLEMENT', `Earnings for Order #${order.id} (minus fees)`, order.id, 'available', {
      item_price: itemPrice,
      fee: fee,
      fee_payer: order.fee_payer
    });

    // 4. Platform Credit (if fee paid by SELLER, it's captured now)
    if (order.fee_payer === 'SELLER' && fee > 0) {
        const pWalletId = await ensureWalletExists(client, 'PLATFORM', 'SYSTEM');
        await recordEntry(client, pWalletId, 'CREDIT', fee, 'SECURE_PAY_FEE', `Escrow service fee from Seller for Order #${order.id}`, order.id);
    }

    // 5. Update Order Status
    await client.query(
      "UPDATE orders SET escrow_status = 'released', status = 'RELEASED' WHERE id = $1",
      [orderId]
    );

    if (shouldRelease) await client.query('COMMIT');
    console.log(`[Wallet] Escrow Released for Order #${orderId}. Seller Payout: ${sellerPayout} to User ${targetUserId}`);
  } catch (error) {
    if (shouldRelease) await client.query('ROLLBACK');
    console.error('[Wallet] Escrow Release Failed:', error.message);
    throw error;
  } finally {
    if (shouldRelease) client.release();
  }
};

/**
 * Refunds an escrow payment to the buyer.
 */
const refundEscrow = async (orderId, providedClient = null) => {
    const client = providedClient || await db.pool.connect();
    let shouldRelease = !providedClient;

    try {
        if (shouldRelease) await client.query('BEGIN');

        const { rows } = await client.query(
            `SELECT o.id, o.fulfiller_id, o.seller_id, o.item_price, o.escrow_status, f.user_id as fulfiller_user_id
             FROM orders o
             LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
             WHERE o.id = $1 FOR UPDATE`,
            [orderId]
        );
        const order = rows[0];

        if (!order || order.escrow_status !== 'held') {
            throw new Error('Order not eligible for refund');
        }

        const itemPrice = parseFloat(order.item_price);

        const targetUserId = order.seller_id || order.fulfiller_user_id || order.user_id;
        const sellerWalletId = await ensureWalletExists(client, 'USER', targetUserId);

        // Debit Seller's Pending (zeros out the hold)
        await recordEntry(client, sellerWalletId, 'DEBIT', itemPrice, 'ESCROW_REFUND', `Refunding escrow for Order #${order.id}`, order.id, 'pending');

        // Update Order Status
        await client.query(
            "UPDATE orders SET escrow_status = 'refunded', status = 'REFUNDED' WHERE id = $1",
            [orderId]
        );

        if (shouldRelease) await client.query('COMMIT');
        console.log(`[Wallet] Escrow Refunded for Order #${orderId}. Amount: ${itemPrice} from User ${targetUserId}`);
    } catch (error) {
        if (shouldRelease) await client.query('ROLLBACK');
        console.error('[Wallet] Escrow Refund Failed:', error.message);
        throw error;
    } finally {
        if (shouldRelease) client.release();
    }
};

/**
 * Awards referral rewards.
 */
const processReferralReward = async (client, userId) => {
    try {
        const { rows } = await client.query(
            "SELECT referred_by_user_id FROM users WHERE id = $1 AND email_verified_at IS NOT NULL",
            [userId]
        );
        const referrerId = rows[0]?.referred_by_user_id;
        if (!referrerId) return;

        const REWARD_AMOUNT = 250;
        const referrerWalletId = await ensureWalletExists(client, 'USER', referrerId);
        await recordEntry(client, referrerWalletId, 'CREDIT', REWARD_AMOUNT, 'REFERRAL_BONUS', `Bonus for referring user #${userId}`);

        const userWalletId = await ensureWalletExists(client, 'USER', userId);
        await recordEntry(client, userWalletId, 'CREDIT', REWARD_AMOUNT, 'REFERRAL_WELCOME', `Welcome bonus for using referral code`);

        await client.query(
            "INSERT INTO referrals (referrer_id, referred_id, status, rewarded_at) VALUES ($1, $2, 'completed', CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
            [referrerId, userId]
        );
    } catch (error) {
        console.error('[Growth] Referral Error:', error.message);
    }
};

/**
 * Awards loyalty points based on spent amount.
 */
const awardLoyaltyPoints = async (client, userId, amount) => {
    try {
        const points = Math.floor(parseFloat(amount) / 100);
        if (points <= 0) return;
        await client.query(
            "INSERT INTO loyalty_ledger (user_id, points, entry_type, description) VALUES ($1, $2, 'EARN', $3)",
            [userId, points, `Earned from mission spending`]
        );
    } catch (error) {
        console.error('[Growth] Loyalty Error:', error.message);
    }
};

module.exports = {
  ensureWalletExists,
  recordEntry,
  processMissionSettlement,
  processCoDRemittance,
  releaseEscrow,
  refundEscrow,
  processReferralReward,
  awardLoyaltyPoints
};
