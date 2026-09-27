const db = require('./src/config/db');
const walletService = require('./src/services/walletService');

async function reconcileWalletsAndMissions() {
    console.log("=================================================");
    console.log("🚀 PIKOP V3 DATA RECONCILIATION & HEALING UTILITY");
    console.log("=================================================\n");

    const client = await db.pool.connect();

    try {
        // ----------------------------------------------------
        // STAGE 0: PURGE ORPHANED WALLETS (DELETED/NON-EXISTENT ACCOUNTS)
        // ----------------------------------------------------
        console.log("--- STAGE 0: Purging Orphaned Wallets for Deleted/Non-Existent Entities ---");
        await client.query('BEGIN');

        // 1. User Wallets without valid user row
        const { rows: orphanedUserWallets } = await client.query(`
            SELECT w.id, w.owner_id, w.owner_type, w.balance, w.pending_balance
            FROM wallets w
            WHERE w.owner_type = 'USER'
              AND w.owner_id ~ '^[0-9]+$'
              AND w.owner_id::integer NOT IN (SELECT id FROM users)
        `);

        // 2. Fulfiller Wallets without valid fulfiller row
        const { rows: orphanedFulfillerWallets } = await client.query(`
            SELECT w.id, w.owner_id, w.owner_type, w.balance, w.pending_balance
            FROM wallets w
            WHERE w.owner_type = 'FULFILLER'
              AND w.owner_id ~ '^[0-9]+$'
              AND w.owner_id::integer NOT IN (SELECT id FROM fulfillers)
        `);

        // 3. Merchant / Vendor / Kitchen Wallets without valid entity or user row
        const { rows: orphanedMerchantWallets } = await client.query(`
            SELECT w.id, w.owner_id, w.owner_type, w.balance, w.pending_balance
            FROM wallets w
            WHERE w.owner_type IN ('MERCHANT', 'VENDOR', 'KITCHEN')
              AND w.owner_id ~ '^[0-9]+$'
              AND w.owner_id::integer NOT IN (SELECT id FROM merchant_accounts)
              AND w.owner_id::integer NOT IN (SELECT id FROM vendors)
              AND w.owner_id::integer NOT IN (SELECT id FROM kitchens)
              AND w.owner_id::integer NOT IN (SELECT id FROM users)
        `);

        // 4. Corporate Wallets without valid corporate account
        const { rows: orphanedCorporateWallets } = await client.query(`
            SELECT w.id, w.owner_id, w.owner_type, w.balance, w.pending_balance
            FROM wallets w
            WHERE w.owner_type = 'CORPORATE'
              AND w.owner_id ~ '^[0-9]+$'
              AND w.owner_id::integer NOT IN (SELECT id FROM corporate_accounts)
        `);

        const allOrphans = [
            ...orphanedUserWallets,
            ...orphanedFulfillerWallets,
            ...orphanedMerchantWallets,
            ...orphanedCorporateWallets
        ];

        let purgedCount = 0;
        let purgedBalanceTotal = 0;

        for (const ow of allOrphans) {
            // Delete withdrawals associated with this orphaned wallet
            await client.query("DELETE FROM withdrawals WHERE wallet_id = $1", [ow.id]);
            // Delete wallet (wallet_ledger_entries cascades)
            await client.query("DELETE FROM wallets WHERE id = $1", [ow.id]);
            purgedCount++;
            purgedBalanceTotal += parseFloat(ow.balance || 0);
            console.log(`  [Purge] Deleted Orphaned Wallet ${ow.id} (${ow.owner_type}:${ow.owner_id}) with Balance: ₦${parseFloat(ow.balance || 0).toFixed(2)}`);
        }

        await client.query('COMMIT');
        console.log(`✅ Stage 0 complete. Purged ${purgedCount} orphaned wallets (Total Purged Balance: ₦${purgedBalanceTotal.toFixed(2)}).\n`);

        // ----------------------------------------------------
        // STAGE 1: UNIFY FULFILLER WALLETS INTO USER WALLETS
        // ----------------------------------------------------
        console.log("--- STAGE 1: Unifying Fulfiller & User Wallets ---");
        await client.query('BEGIN');

        const { rows: fulfillerWallets } = await client.query(
            "SELECT * FROM wallets WHERE owner_type = 'FULFILLER'"
        );

        let mergedCount = 0;
        for (const fw of fulfillerWallets) {
            const fRes = await client.query("SELECT user_id FROM fulfillers WHERE id = $1", [fw.owner_id]);
            if (fRes.rows.length > 0) {
                const targetUserId = fRes.rows[0].user_id;
                const targetWalletId = await walletService.ensureWalletExists(client, 'USER', targetUserId);

                if (targetWalletId !== fw.id) {
                    // Move ledger entries
                    await client.query("UPDATE wallet_ledger_entries SET wallet_id = $1 WHERE wallet_id = $2", [targetWalletId, fw.id]);
                    // Move withdrawals
                    await client.query("UPDATE withdrawals SET wallet_id = $1 WHERE wallet_id = $2", [targetWalletId, fw.id]);
                    // Merge balances
                    await client.query(
                        "UPDATE wallets SET balance = balance + $1, pending_balance = pending_balance + $2, updated_at = NOW() WHERE id = $3",
                        [parseFloat(fw.balance || 0), parseFloat(fw.pending_balance || 0), targetWalletId]
                    );
                    // Delete legacy fulfiller wallet
                    await client.query("DELETE FROM wallets WHERE id = $1", [fw.id]);
                    mergedCount++;
                    console.log(`  [Merge] Fulfiller Wallet ${fw.id} (owner ${fw.owner_id}) merged into User Wallet ${targetWalletId} (user ${targetUserId})`);
                }
            }
        }
        await client.query('COMMIT');
        console.log(`✅ Stage 1 complete. Merged ${mergedCount} legacy fulfiller wallets.\n`);

        // ----------------------------------------------------
        // STAGE 2: RECONCILE FULFILLER LINKAGES ON DELIVERED ORDERS
        // ----------------------------------------------------
        console.log("--- STAGE 2: Reconciling Missing Fulfiller Linkages ---");
        await client.query('BEGIN');

        let relinkedCount = 0;

        // Case A: Fulfiller ID is missing on DELIVERED order, but queued_for_fulfiller_id exists
        const { rows: unlinkedQueued } = await client.query(`
            SELECT id, queued_for_fulfiller_id FROM orders
            WHERE status = 'DELIVERED' AND fulfiller_id IS NULL AND queued_for_fulfiller_id IS NOT NULL
        `);

        for (const uq of unlinkedQueued) {
            await client.query(
                "UPDATE orders SET fulfiller_id = $1 WHERE id = $2",
                [uq.queued_for_fulfiller_id, uq.id]
            );
            relinkedCount++;
            console.log(`  [Relink] Order #${uq.id} linked to Fulfiller #${uq.queued_for_fulfiller_id} (from queue)`);
        }

        // Case B: Fulfiller ID missing on DELIVERED order, resolve from chat messages
        const { rows: unlinkedMessages } = await client.query(`
            SELECT DISTINCT m.order_id, m.sender_id
            FROM messages m
            JOIN orders o ON o.id = m.order_id
            WHERE o.status = 'DELIVERED' AND o.fulfiller_id IS NULL AND m.sender_type = 'FULFILLER'
        `);

        for (const um of unlinkedMessages) {
            const fRes = await client.query("SELECT id FROM fulfillers WHERE user_id = $1", [um.sender_id]);
            if (fRes.rows.length > 0) {
                await client.query("UPDATE orders SET fulfiller_id = $1 WHERE id = $2", [fRes.rows[0].id, um.order_id]);
                relinkedCount++;
                console.log(`  [Relink] Order #${um.order_id} linked to Fulfiller #${fRes.rows[0].id} (from chat history)`);
            }
        }

        await client.query('COMMIT');
        console.log(`✅ Stage 2 complete. Relinked ${relinkedCount} orders to fulfillers.\n`);

        // ----------------------------------------------------
        // STAGE 3: BACKFILL MISSING MISSION SETTLEMENTS (75% FARE SHARE)
        // ----------------------------------------------------
        console.log("--- STAGE 3: Backfilling Missing Mission Settlements ---");

        const { rows: deliveredOrders } = await client.query(`
            SELECT o.id, o.fulfiller_id, o.total_fare, o.delivery_fee, o.created_at
            FROM orders o
            WHERE o.status = 'DELIVERED' AND o.fulfiller_id IS NOT NULL
            ORDER BY o.created_at ASC
        `);

        let settledCount = 0;
        for (const order of deliveredOrders) {
            // Check if SETTLEMENT ledger entry exists for this order
            const { rows: existing } = await client.query(
                "SELECT id FROM wallet_ledger_entries WHERE order_id = $1 AND purpose = 'SETTLEMENT'",
                [order.id]
            );

            if (existing.length === 0) {
                try {
                    await walletService.processMissionSettlement(order.id);
                    settledCount++;
                    console.log(`  [Settlement] Backfilled 75% mission settlement for Delivered Order #${order.id} (Fare: ₦${order.delivery_fee || order.total_fare})`);
                } catch (sErr) {
                    console.error(`  ⚠️ Settlement failed for Order #${order.id}:`, sErr.message);
                }
            }
        }

        console.log(`✅ Stage 3 complete. Backfilled ${settledCount} missing mission settlements.\n`);

        // ----------------------------------------------------
        // STAGE 4: RECALCULATE & SYNC ALL WALLET BALANCES FROM LEDGER
        // ----------------------------------------------------
        console.log("--- STAGE 4: Recalculating Wallet Balances from Ledger ---");
        await client.query('BEGIN');

        const { rows: allWallets } = await client.query("SELECT id, owner_type, owner_id, balance FROM wallets");

        let adjustedCount = 0;
        for (const w of allWallets) {
            const { rows: calcRes } = await client.query(`
                SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) as calc_balance
                FROM wallet_ledger_entries
                WHERE wallet_id = $1
            `, [w.id]);

            const newBalance = parseFloat(calcRes[0].calc_balance || 0);
            const oldBalance = parseFloat(w.balance || 0);

            if (Math.abs(newBalance - oldBalance) > 0.001) {
                await client.query(
                    "UPDATE wallets SET balance = $1, updated_at = NOW() WHERE id = $2",
                    [newBalance, w.id]
                );
                adjustedCount++;
                console.log(`  [Sync] Wallet ${w.id} (${w.owner_type}:${w.owner_id}) balance synced: ₦${oldBalance.toFixed(2)} -> ₦${newBalance.toFixed(2)}`);
            }
        }

        await client.query('COMMIT');
        console.log(`✅ Stage 4 complete. Recalculated and adjusted ${adjustedCount} wallet balances.\n`);

        console.log("=================================================");
        console.log("✨ ALL WALLET BALANCES AND MISSION HISTORIES RECONCILED");
        console.log("=================================================");

    } catch (e) {
        await client.query('ROLLBACK');
        console.error("❌ RECONCILIATION FATAL ERROR:", e.message, e.stack);
    } finally {
        client.release();
        process.exit(0);
    }
}

reconcileWalletsAndMissions();
