/**
 * One-off repair for issue #11 (wallet permanently stuck at zero/NaN).
 *
 * Root cause (fixed in orderController.js createOrder, the $35 INSERT param):
 * for recipient-pays orders, `q.delivery_fee` is undefined on the quote
 * object, and `parseFloat(q.delivery_fee)` with no fallback evaluated to
 * NaN, which got written directly into orders.original_delivery_fee (a
 * NUMERIC column, which Postgres allows to hold the literal value NaN).
 * walletService's processMissionSettlement() then read that NaN straight
 * into the agent's SETTLEMENT/COMMISSION ledger entries, and because
 * NaN + anything = NaN in Postgres NUMERIC arithmetic, every later addition
 * to that same wallet's `balance` column stayed NaN forever, regardless of
 * how correct the later transactions were.
 *
 * This script is SAFE to run: a NaN balance could never pass a
 * `balance >= amount` withdrawal check (NaN comparisons are always false in
 * JS), so no agent could have been incorrectly paid out against a broken
 * balance while this bug was live -- there is no real money to reconcile,
 * only bad bookkeeping to correct.
 *
 * IMPORTANT DESIGN CHOICE: this script does NOT delete or replay every
 * ledger entry on an affected wallet, and does NOT call the full
 * processMissionSettlement() again. Entries like PEAK_BONUS/STREAK_BONUS
 * are time-of-day and current-streak dependent -- replaying them today
 * would recompute them against TODAY's clock/streak state, not the
 * original order's, and could duplicate a bonus that was already correctly
 * recorded (its `amount` was a real number; only the wallet's running
 * `balance` column was corrupted downstream of it, not the entry itself).
 * So it only touches entries whose own `amount` is literally NaN (the
 * actually-wrong SETTLEMENT/COMMISSION transactions), recomputes those two
 * specific amounts deterministically from the (now-fixed) order record
 * using the exact same formula as processMissionSettlement, and leaves
 * every other entry (SMS_CHARGE, PEAK_BONUS, STREAK_BONUS, etc.) untouched.
 *
 * What it does, in order, inside a single transaction (all or nothing):
 *   1. Resets orders.original_delivery_fee from the literal 'NaN' to 0 for
 *      every affected order (matching the now-fixed creation-time fallback).
 *   2. Finds every wallet_ledger_entries row whose AMOUNT (not balance_after)
 *      is the literal 'NaN' -- these are the genuinely-wrong SETTLEMENT/
 *      COMMISSION transactions -- and deletes only those rows.
 *   3. Recomputes every affected wallet's `balance` as the sum of all its
 *      REMAINING ledger entries (all of which now have real, valid amounts),
 *      replacing the corrupted NaN value with a correct baseline.
 *   4. For each order whose SETTLEMENT/COMMISSION entry was deleted in step
 *      2, recomputes the correct fulfillerShare/platformShare using the
 *      same math as processMissionSettlement() (now safe, since
 *      original_delivery_fee is fixed) and inserts fresh SETTLEMENT /
 *      COMMISSION entries via walletService.recordEntry(), which also
 *      correctly updates the now-clean wallet balance.
 *
 * Usage:
 *   node scripts/repair_nan_wallets.js            # DRY RUN -- prints what
 *                                                   it would do, changes nothing
 *   node scripts/repair_nan_wallets.js --apply     # actually applies the fix
 */
const db = require('../src/config/db');
const { recordEntry, ensureWalletExists } = require('../src/services/walletService');

const APPLY = process.argv.includes('--apply');

(async () => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    console.log(APPLY ? '=== APPLYING REPAIR ===' : '=== DRY RUN (pass --apply to actually fix) ===');

    // 1. Find + report orders with corrupted original_delivery_fee
    const badOrders = await client.query(
      `SELECT id, delivery_fee, original_delivery_fee, total_fare, original_total_fare,
              fulfiller_id, dispatch_commission_amount
       FROM orders WHERE original_delivery_fee::text = 'NaN'`
    );
    console.log(`\n[1] Orders with original_delivery_fee = NaN: ${badOrders.rows.length}`);
    badOrders.rows.forEach(o => console.log(`    Order #${o.id} | fulfiller_id: ${o.fulfiller_id} | original_total_fare: ${o.original_total_fare}`));

    // NOTE: this write (and every write below) always runs inside this
    // transaction, even on a dry run -- that's what lets steps 3/4 compute
    // and preview their REAL resulting numbers from the corrected data.
    // The transaction is only ever COMMITted when --apply is passed; a dry
    // run always ROLLBACKs at the very end, so nothing is ever actually
    // persisted unless you pass --apply.
    if (badOrders.rows.length > 0) {
      await client.query(`UPDATE orders SET original_delivery_fee = 0 WHERE original_delivery_fee::text = 'NaN'`);
      console.log('    -> Reset to 0 (within this transaction).');
    }

    // 2. Find ONLY the ledger entries whose own amount is NaN (the actually
    //    wrong transactions) -- leave every other entry (bonuses, SMS
    //    charges, etc.) completely untouched, even if their balance_after
    //    snapshot was corrupted downstream.
    const badEntries = await client.query(
      `SELECT id, wallet_id, order_id, entry_type, purpose FROM wallet_ledger_entries
       WHERE amount::text = 'NaN'`
    );
    console.log(`\n[2] Ledger entries with amount = NaN (to be replaced): ${badEntries.rows.length}`);
    badEntries.rows.forEach(e => console.log(`    Entry #${e.id} | Order #${e.order_id} | ${e.purpose} | ${e.entry_type} | wallet ${e.wallet_id}`));

    const affectedOrderIds = [...new Set(badEntries.rows.map(r => r.order_id))];
    const affectedWalletIds = [...new Set(badEntries.rows.map(r => r.wallet_id))];

    if (badEntries.rows.length > 0) {
      await client.query(`DELETE FROM wallet_ledger_entries WHERE amount::text = 'NaN'`);
      console.log('    -> Deleted (within this transaction).');
    }

    // 3. Recompute balance for every affected wallet from its REMAINING
    //    (all now-valid) ledger entries. This gives a correct baseline
    //    before we insert any new entries in step 4.
    console.log(`\n[3] Recomputing balances for ${affectedWalletIds.length} affected wallet(s):`);
    for (const walletId of affectedWalletIds) {
      const sumRes = await client.query(
        `SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) AS computed_balance
         FROM wallet_ledger_entries WHERE wallet_id = $1`,
        [walletId]
      );
      const computed = sumRes.rows[0].computed_balance;
      const current = await client.query(`SELECT balance FROM wallets WHERE id = $1`, [walletId]);
      console.log(`    Wallet ${walletId}: current balance = ${current.rows[0]?.balance} -> recomputed baseline = ${computed}`);
      await client.query(`UPDATE wallets SET balance = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`, [computed, walletId]);
    }

    // 4. For each affected order, recompute the correct SETTLEMENT
    //    (fulfiller share) and COMMISSION (platform share) using the exact
    //    same formula as walletService.processMissionSettlement(), now
    //    safe because original_delivery_fee no longer contains NaN. This
    //    does NOT touch peak-hour/streak bonuses -- those entries, if
    //    valid, were left alone in step 2 and need no replay.
    console.log(`\n[4] Re-crediting correct SETTLEMENT/COMMISSION for ${affectedOrderIds.length} order(s):`);
    for (const orderId of affectedOrderIds) {
      const orderRes = await client.query(
        `SELECT o.id, o.fulfiller_id, o.original_delivery_fee, o.delivery_fee, o.total_fare,
                o.dispatch_commission_amount, o.fee_payer, o.platform_fee_amount, o.sms_charge_amount,
                COALESCE(f1.user_id, f2.user_id, o.fulfiller_id) as fulfiller_user_id,
                COALESCE(f1.fleet_partner_id, f2.fleet_partner_id) as fleet_partner_id,
                COALESCE(fp1.commission_override, fp2.commission_override) as fleet_commission_override
         FROM orders o
         LEFT JOIN fulfillers f1 ON f1.id = o.fulfiller_id
         LEFT JOIN fulfillers f2 ON f2.user_id = o.fulfiller_id
         LEFT JOIN fleet_partners fp1 ON fp1.id = f1.fleet_partner_id
         LEFT JOIN fleet_partners fp2 ON fp2.id = f2.fleet_partner_id
         WHERE o.id = $1`,
        [orderId]
      );
      const order = orderRes.rows[0];
      if (!order || !order.fulfiller_id || !order.fulfiller_user_id) {
        console.log(`    Order #${orderId}: SKIPPED (no fulfiller/user link -- cannot settle)`);
        continue;
      }

      const settlableAmount = parseFloat(order.original_delivery_fee || order.delivery_fee || order.total_fare || 0);

      let platformShare;
      if (order.dispatch_commission_amount && parseFloat(order.dispatch_commission_amount) > 0) {
        platformShare = parseFloat(order.dispatch_commission_amount);
        if (order.fleet_commission_override !== null) {
          platformShare = settlableAmount * parseFloat(order.fleet_commission_override);
        }
      } else {
        const settingsRes = await client.query("SELECT value FROM settings WHERE key = 'platform_commission'");
        const commissionRate = parseFloat(settingsRes.rows[0]?.value || '0.25');
        platformShare = settlableAmount * commissionRate;
      }
      const fulfillerShare = settlableAmount - platformShare;

      console.log(`    Order #${orderId}: settlableAmount=${settlableAmount} platformShare=${platformShare} fulfillerShare=${fulfillerShare}`);

      const fWalletId = await ensureWalletExists(client, 'USER', order.fulfiller_user_id);
      await recordEntry(client, fWalletId, 'CREDIT', fulfillerShare, 'SETTLEMENT', `Earnings for Mission #${order.id} (repaired)`, order.id);

      const pWalletId = await ensureWalletExists(client, 'PLATFORM', 'SYSTEM');
      await recordEntry(client, pWalletId, 'CREDIT', platformShare, 'COMMISSION', `Commission for Mission #${order.id} (repaired)`, order.id);
      console.log(`    -> Credited (within this transaction).`);
    }

    // Show the FINAL post-repair balances for every affected wallet, so the
    // dry-run preview is actually useful -- this reflects steps 1-4 exactly
    // as they would land if you re-run this with --apply.
    console.log(`\n[5] Final wallet balances after repair (preview):`);
    for (const walletId of affectedWalletIds) {
      const finalRes = await client.query(`SELECT balance FROM wallets WHERE id = $1`, [walletId]);
      console.log(`    Wallet ${walletId}: balance = ${finalRes.rows[0]?.balance}`);
    }

    if (APPLY) {
      await client.query('COMMIT');
      console.log('\n=== DONE. All changes committed. ===');
    } else {
      await client.query('ROLLBACK');
      console.log('\n=== DRY RUN complete, nothing was changed. Review the numbers above, then re-run with --apply to fix for real. ===');
    }
  } catch (e) {
    await client.query('ROLLBACK');
    console.error('\n!!! ERROR -- rolled back, nothing was changed:', e);
    process.exitCode = 1;
  } finally {
    client.release();
    await db.pool.end();
  }
})();
