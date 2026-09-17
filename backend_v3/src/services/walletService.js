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
                o.dispatch_commission_amount,
                f.user_id as fulfiller_user_id, f.fleet_partner_id,
                fp.commission_override as fleet_commission_override
         FROM orders o
         LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
         LEFT JOIN fleet_partners fp ON fp.id = f.fleet_partner_id
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

    // 2. Fetch Split Config or use Frozen Amount (v3.9.8)
    let platformShare;
    if (order.dispatch_commission_amount && parseFloat(order.dispatch_commission_amount) > 0) {
        platformShare = parseFloat(order.dispatch_commission_amount);

        // 2.1 Check for Fleet Override (v4.1)
        if (order.fleet_commission_override !== null) {
            const overrideRate = parseFloat(order.fleet_commission_override);
            platformShare = settlableAmount * overrideRate;
            console.log(`[Wallet] Applying FLEET OVERRIDE (${overrideRate * 100}%) for Order #${orderId}`);
        } else {
            console.log(`[Wallet] Using FROZEN commission for Order #${orderId}: ${platformShare}`);
        }
    } else {
        const settingsRes = await client.query("SELECT value FROM settings WHERE key = 'platform_commission'");
        const commissionRate = parseFloat(settingsRes.rows[0]?.value || '0.25');
        platformShare = settlableAmount * commissionRate;
        console.log(`[Wallet] Using DYNAMIC commission for legacy Order #${orderId}: ${platformShare}`);
    }

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

    // 7. Insurance Premium (v4.3)
    if (order.is_insured && parseFloat(order.insurance_fee) > 0) {
        await recordEntry(client, pWalletId, 'CREDIT', order.insurance_fee, 'INSURANCE_PREMIUM', `Insurance protection for Order #${order.id}`, order.id);
    }

    // 8. Fulfiller Incentives (v4.4)
    try {
        const { getWATTimeStr, isWithinWindow } = require('../utils/time');
        const fcmService = require('./fcmService');
        const settingsRes = await client.query("SELECT key, value FROM settings WHERE key IN ('peak_hour_start', 'peak_hour_end', 'peak_hour_bonus', 'streak_bonus_7_day', 'streak_bonus_30_day')");
        const s = {};
        settingsRes.rows.forEach(r => s[r.key] = r.value);

        // Peak Hour Check
        const nowTime = getWATTimeStr();
        if (isWithinWindow(nowTime, s['peak_hour_start'] || '16:00', s['peak_hour_end'] || '19:00')) {
            const peakBonus = parseFloat(s['peak_hour_bonus'] || '300');
            if (peakBonus > 0) {
                await recordEntry(client, fWalletId, 'CREDIT', peakBonus, 'PEAK_BONUS', `Peak Hour Bonus for Order #${order.id}`, order.id);
                await recordEntry(client, pWalletId, 'DEBIT', peakBonus, 'PEAK_BONUS', `Peak Hour Bonus payout for Order #${order.id}`, order.id);
            }
        }

        // Streak Check
        const { rows: fRows } = await client.query("SELECT current_streak_days, last_streak_date FROM fulfillers WHERE id = $1 FOR UPDATE", [order.fulfiller_id]);
        if (fRows.length > 0) {
            const f = fRows[0];
            const today = new Date().toISOString().split('T')[0];
            const lastDate = f.last_streak_date ? new Date(f.last_streak_date).toISOString().split('T')[0] : null;

            if (lastDate !== today) {
                const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0];
                let newStreak = (lastDate === yesterday) ? parseInt(f.current_streak_days) + 1 : 1;

                await client.query("UPDATE fulfillers SET current_streak_days = $1, last_streak_date = CURRENT_DATE WHERE id = $2", [newStreak, order.fulfiller_id]);

                // Award bonuses at milestones
                let streakBonus = 0;
                if (newStreak === 7) streakBonus = parseFloat(s['streak_bonus_7_day'] || '1000');
                else if (newStreak === 30) streakBonus = parseFloat(s['streak_bonus_30_day'] || '5000');

                if (streakBonus > 0) {
                    await recordEntry(client, fWalletId, 'CREDIT', streakBonus, 'STREAK_BONUS', `${newStreak}-Day Streak Bonus!`, order.id);
                    await recordEntry(client, pWalletId, 'DEBIT', streakBonus, 'STREAK_BONUS', `${newStreak}-Day Streak payout`, order.id);
                    fcmService.sendNotification(order.fulfiller_user_id, "Streak Bonus Unlocked! 🔥", `You hit a ${newStreak}-day streak and earned ₦${streakBonus}!`, { type: 'WALLET_UPDATE' });
                }
            }
        }
    } catch (incErr) {
        console.error('[Incentives] Error:', incErr.message);
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

    // 1. Lock the order row first (prevents race conditions)
    await client.query("SELECT id FROM orders WHERE id = $1 FOR UPDATE", [orderId]);

    // 2. Fetch order details with fulfiller info
    const orderRes = await client.query(
      `SELECT o.id, o.fulfiller_id, o.item_price, o.platform_fee_amount, o.fee_payer, o.user_id, o.seller_id,
              o.escrow_status, f.user_id as fulfiller_user_id, o.merchant_commission_amount
       FROM orders o
       LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
       WHERE o.id = $1`,
      [orderId]
    );
    const order = orderRes.rows[0];

    if (!order || order.escrow_status !== 'held') {
      throw new Error('Order not eligible for escrow release');
    }

    const itemPrice = parseFloat(order.item_price);
    const fee = parseFloat(order.platform_fee_amount);
    const marketplaceCommission = parseFloat(order.merchant_commission_amount || 0);

    // 2. Determine Seller Payout
    // If fee_payer is SELLER, they bear the escrow fee too (rare in current rules, but supported)
    let sellerPayout = order.fee_payer === 'SELLER' ? (itemPrice - fee) : itemPrice;

    // Deduct Marketplace Commission (Always borne by seller, regardless of escrow fee rules)
    sellerPayout = sellerPayout - marketplaceCommission;

    // 3. Update Seller Wallet (ALWAYS USER TYPE NOW)
    const targetUserId = order.seller_id || order.fulfiller_user_id || order.user_id;
    const sellerWalletId = await ensureWalletExists(client, 'USER', targetUserId);

    // Debit Pending
    await recordEntry(client, sellerWalletId, 'DEBIT', itemPrice, 'ESCROW_RELEASE', `Releasing escrow for Order #${order.id}`, order.id, 'pending');

    // Credit Available
    await recordEntry(client, sellerWalletId, 'CREDIT', sellerPayout, 'SETTLEMENT', `Earnings for Order #${order.id} (minus fees)`, order.id, 'available', {
      item_price: itemPrice,
      escrow_fee: fee,
      marketplace_commission: marketplaceCommission,
      fee_payer: order.fee_payer
    });

    const pWalletId = await ensureWalletExists(client, 'PLATFORM', 'SYSTEM');

    // 4. Platform Credit for Escrow Fee (if fee paid by SELLER)
    if (order.fee_payer === 'SELLER' && fee > 0) {
        await recordEntry(client, pWalletId, 'CREDIT', fee, 'SECURE_PAY_FEE', `Escrow service fee from Seller for Order #${order.id}`, order.id);
    }

    // 4.1 Platform Credit for Marketplace Commission
    if (marketplaceCommission > 0) {
        await recordEntry(client, pWalletId, 'CREDIT', marketplaceCommission, 'COMMISSION', `Marketplace Commission for Order #${order.id}`, order.id);
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

        // 1. Lock the order row first
        await client.query("SELECT id FROM orders WHERE id = $1 FOR UPDATE", [orderId]);

        // 2. Fetch details
        const { rows } = await client.query(
            `SELECT o.id, o.fulfiller_id, o.seller_id, o.item_price, o.escrow_status, f.user_id as fulfiller_user_id
             FROM orders o
             LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
             WHERE o.id = $1`,
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
 * Awards referral rewards with Anti-Abuse checks (v4.4).
 */
const processReferralReward = async (client, userId) => {
    try {
        const { rows } = await client.query(
            "SELECT u.referred_by_user_id, u.phone, u.email, (SELECT phone FROM users WHERE id = u.referred_by_user_id) as referrer_phone FROM users u WHERE u.id = $1 AND u.email_verified_at IS NOT NULL",
            [userId]
        );
        const refInfo = rows[0];
        if (!refInfo || !refInfo.referred_by_user_id) return;

        // Anti-Abuse: Prevent same phone number
        if (refInfo.referrer_phone === refInfo.phone) {
            console.warn(`[Growth] Referral Abuse Blocked: Same phone for User ${userId} and Referrer ${refInfo.referred_by_user_id}`);
            return;
        }

        const referrerId = refInfo.referred_by_user_id;
        const REWARD_AMOUNT = 250;

        const referrerWalletId = await ensureWalletExists(client, 'USER', referrerId);
        await recordEntry(client, referrerWalletId, 'CREDIT', REWARD_AMOUNT, 'REFERRAL_BONUS', `Bonus for referring user #${userId}`);

        const userWalletId = await ensureWalletExists(client, 'USER', userId);
        await recordEntry(client, userWalletId, 'CREDIT', REWARD_AMOUNT, 'REFERRAL_WELCOME', `Welcome bonus for using referral code`);

        await client.query(
            "INSERT INTO referrals (referrer_id, referred_id, status, rewarded_at) VALUES ($1, $2, 'completed', CURRENT_TIMESTAMP) ON CONFLICT (referrer_id, referred_id) DO UPDATE SET status = 'completed', rewarded_at = CURRENT_TIMESTAMP",
            [referrerId, userId]
        );
    } catch (error) {
        console.error('[Growth] Referral Error:', error.message);
    }
};

/**
 * Awards loyalty points based on spent amount and tracks total orders.
 */
const awardLoyaltyPoints = async (client, userId, amount) => {
    try {
        // Increment global order count
        await client.query("UPDATE users SET total_orders_completed = total_orders_completed + 1 WHERE id = $1", [userId]);

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

/**
 * Applies an automated financial waiver (Milestone 35).
 * Reverses penalties or fees based on valid incident reports.
 */
const applyAutomatedWaiver = async (orderId, waiverType, providedClient = null) => {
    const client = providedClient || await db.pool.connect();
    let shouldRelease = !providedClient;

    try {
        if (shouldRelease) await client.query('BEGIN');

        // 1. Fetch Order and User
        const { rows } = await client.query("SELECT id, user_id, total_fare FROM orders WHERE id = $1", [orderId]);
        if (rows.length === 0) throw new Error('Order not found');
        const order = rows[0];

        const walletId = await ensureWalletExists(client, 'USER', order.user_id);
        let amountToReverse = 0;
        let purpose = 'PENALTY_WAIVER';
        let description = '';

        if (waiverType === 'CANCELLATION') {
            amountToReverse = parseFloat(order.total_fare) * 0.25;
            description = `Waiver of 25% cancellation penalty for Mission #${orderId}`;
        } else if (waiverType === 'RETURN') {
            amountToReverse = parseFloat(order.total_fare) * 0.75;
            purpose = 'RETURN_WAIVER';
            description = `Waiver of 75% return fee for Mission #${orderId}`;
        }

        if (amountToReverse > 0) {
            await recordEntry(client, walletId, 'CREDIT', amountToReverse, purpose, description, orderId);
            console.log(`[Waiver] Applied ${waiverType} waiver for User ${order.user_id} on Order ${orderId}: +₦${amountToReverse}`);
        }

        if (shouldRelease) await client.query('COMMIT');
        return { success: true, amount: amountToReverse };
    } catch (error) {
        if (shouldRelease) await client.query('ROLLBACK');
        console.error('[Waiver] Failed:', error.message);
        throw error;
    } finally {
        if (shouldRelease) client.release();
    }
};

/**
 * Processes a refund for a returned item.
 */
const processReturnRefund = async (returnId) => {
    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Fetch Return & Original Order
        const { rows } = await client.query(`
            SELECT r.id, o.id as order_id, o.user_id, o.item_price, o.seller_id, o.fulfiller_id,
                   f.user_id as fulfiller_user_id
            FROM returns r
            JOIN orders o ON o.id = r.order_id
            LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
            WHERE r.id = $1
        `, [returnId]);

        if (rows.length === 0) throw new Error('Return not found');
        const r = rows[0];

        // 2. Resolve Target Seller Wallet
        const sellerId = r.seller_id || r.fulfiller_user_id;
        const sellerWalletId = await ensureWalletExists(client, 'USER', sellerId);
        const buyerWalletId = await ensureWalletExists(client, 'USER', r.user_id);

        // 3. Move Item Price back to Buyer
        // NOTE: We only refund the ITEM PRICE. The delivery fee for both legs is already consumed by agents.
        const refundAmount = parseFloat(r.item_price);

        await recordEntry(client, sellerWalletId, 'DEBIT', refundAmount, 'SETTLEMENT', `Refund for returned item (Return #${returnId})`, r.order_id);
        await recordEntry(client, buyerWalletId, 'CREDIT', refundAmount, 'SETTLEMENT', `Refund for returned item #${r.order_id}`, r.order_id);

        await client.query("UPDATE returns SET status = 'COMPLETED' WHERE id = $1", [returnId]);

        await client.query('COMMIT');
        console.log(`[Refund] Processed return refund for Order ${r.order_id}: ₦${refundAmount} from ${sellerId} to ${r.user_id}`);

    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Refund] Return Refund Failed:', error.message);
        throw error;
    } finally {
        client.release();
    }
};

/**
 * Debits a Corporate account for an order (v4.3).
 * Enforces per-user spending limits and wallet availability.
 */
const processCorporateDebit = async (client, corporateAccountId, amount, userId, orderId) => {
    // 1. Lock Account and Wallet
    const { rows: accRes } = await client.query(`
        SELECT ca.id, ca.status, ca.billing_type, w.id as wallet_id, w.balance,
               csa.daily_spend_limit, csa.monthly_spend_limit
        FROM corporate_accounts ca
        JOIN wallets w ON w.corporate_account_id = ca.id
        JOIN corporate_sub_accounts csa ON csa.corporate_account_id = ca.id
        WHERE ca.id = $1 AND csa.user_id = $2
        FOR UPDATE OF ca, w, csa`, [corporateAccountId, userId]);

    if (accRes.length === 0) throw new Error('Corporate account not found or user not authorized.');
    const acc = accRes[0];

    if (acc.status !== 'ACTIVE') throw new Error('Corporate account is not active.');

    // 2. Check Limits
    if (parseFloat(acc.daily_spend_limit) > 0) {
        const { rows: dailyRes } = await client.query(
            "SELECT SUM(total_fare) as spent FROM orders WHERE corporate_account_id = $1 AND user_id = $2 AND created_at >= CURRENT_DATE",
            [corporateAccountId, userId]
        );
        const spentToday = parseFloat(dailyRes[0].spent || 0);
        if (spentToday + amount > parseFloat(acc.daily_spend_limit)) {
            throw new Error(`Daily spend limit exceeded. Remaining: ₦${(parseFloat(acc.daily_spend_limit) - spentToday).toLocaleString()}`);
        }
    }

    if (parseFloat(acc.monthly_spend_limit) > 0) {
        const { rows: monthlyRes } = await client.query(
            "SELECT SUM(total_fare) as spent FROM orders WHERE corporate_account_id = $1 AND user_id = $2 AND created_at >= date_trunc('month', CURRENT_DATE)",
            [corporateAccountId, userId]
        );
        const spentThisMonth = parseFloat(monthlyRes[0].spent || 0);
        if (spentThisMonth + amount > parseFloat(acc.monthly_spend_limit)) {
            throw new Error(`Monthly spend limit exceeded. Remaining: ₦${(parseFloat(acc.monthly_spend_limit) - spentThisMonth).toLocaleString()}`);
        }
    }

    // 3. Perform Debit
    if (parseFloat(acc.balance) < amount) throw new Error('Insufficient corporate funds.');

    await recordEntry(client, acc.wallet_id, 'DEBIT', amount, 'CORPORATE_ORDER', `Corporate delivery #${orderId}`, orderId);

    return true;
};

/**
 * Processes an insurance claim for lost/damaged items (v4.3).
 */
const processInsuranceClaim = async (orderId, claimAmount, providedClient = null) => {
    const client = providedClient || await db.pool.connect();
    let shouldRelease = !providedClient;

    try {
        if (shouldRelease) await client.query('BEGIN');

        const { rows } = await client.query("SELECT user_id FROM orders WHERE id = $1", [orderId]);
        if (rows.length === 0) throw new Error('Order not found');
        const userId = rows[0].user_id;

        const pWalletId = await ensureWalletExists(client, 'PLATFORM', 'SYSTEM');
        const uWalletId = await ensureWalletExists(client, 'USER', userId);

        // Debit Platform (Claims Pool)
        await recordEntry(client, pWalletId, 'DEBIT', claimAmount, 'INSURANCE_CLAIM', `Claim payout for Order #${orderId}`, orderId);

        // Credit User
        await recordEntry(client, uWalletId, 'CREDIT', claimAmount, 'SETTLEMENT', `Insurance claim payout for Order #${orderId}`, orderId);

        if (shouldRelease) await client.query('COMMIT');
        return true;
    } catch (error) {
        if (shouldRelease) await client.query('ROLLBACK');
        console.error('[Insurance] Claim processing failed:', error.message);
        throw error;
    } finally {
        if (shouldRelease) client.release();
    }
};

/**
 * Processes a standard checkout payment using personal wallet balance (v4.6).
 */
const processIndividualWalletPayment = async (client, userId, amount, orderId) => {
    // 1. Lock Wallet
    const walletId = await ensureWalletExists(client, 'USER', userId);
    const { rows } = await client.query("SELECT balance FROM wallets WHERE id = $1 FOR UPDATE", [walletId]);

    if (rows.length === 0) throw new Error('Wallet not found.');
    const currentBalance = parseFloat(rows[0].balance);

    if (currentBalance < amount) {
        throw new Error(`Insufficient wallet balance. Required: ₦${amount.toLocaleString()}. Available: ₦${currentBalance.toLocaleString()}`);
    }

    // 2. Perform Debit
    await recordEntry(client, walletId, 'DEBIT', amount, 'MISSION_PAYMENT', `Payment for mission #${orderId}`, orderId);

    return true;
};

module.exports = {
  ensureWalletExists,
  recordEntry,
  processMissionSettlement,
  processCoDRemittance,
  releaseEscrow,
  refundEscrow,
  processReferralReward,
  awardLoyaltyPoints,
  applyAutomatedWaiver,
  processReturnRefund,
  processCorporateDebit,
  processInsuranceClaim,
  processIndividualWalletPayment
};
