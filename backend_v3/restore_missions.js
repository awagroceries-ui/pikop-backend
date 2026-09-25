const db = require('./src/config/db');

async function restoreMissions() {
    console.log("=== PIKOP MISSION RESTORATION & SYNC UTILITY ===");
    try {
        // 1. Fetch all active/non-finalized orders
        const { rows: orders } = await db.query(`
            SELECT o.id, o.status, o.user_id, o.fulfiller_id, o.queued_for_fulfiller_id,
                   o.pickup_address, o.delivery_address, o.total_fare, o.item_description, o.created_at
            FROM orders o
            WHERE o.status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED')
            ORDER BY o.created_at ASC
        `);

        console.log(`[Restoration] Found ${orders.length} non-finalized missions in database.`);

        let restoredActiveCount = 0;
        let restoredQueuedCount = 0;
        let searchingCount = 0;

        for (const o of orders) {
            // Case A: Has fulfiller_id but status was stuck in SEARCHING / PENDING / PENDING_ACKNOWLEDGMENT
            if (o.fulfiller_id && ['SEARCHING', 'PENDING', 'PENDING_ACKNOWLEDGMENT', 'PAYMENT_CAPTURED', 'PAID', 'PROCESSING', 'CONFIRMED'].includes(o.status)) {
                await db.query(
                    "UPDATE orders SET status = 'MATCHED', matched_at = COALESCE(matched_at, CURRENT_TIMESTAMP) WHERE id = $1",
                    [o.id]
                );
                restoredActiveCount++;
                console.log(`✅ Order #${o.id} restored to MATCHED for Fulfiller #${o.fulfiller_id}`);
            }
            // Case B: Has queued_for_fulfiller_id and no fulfiller_id
            else if (o.queued_for_fulfiller_id && !o.fulfiller_id) {
                // Check if agent currently has an active MATCHED/PICKED_UP mission
                const { rows: activeCheck } = await db.query(
                    "SELECT id FROM orders WHERE fulfiller_id = $1 AND status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED')",
                    [o.queued_for_fulfiller_id]
                );

                if (activeCheck.length === 0) {
                    // Agent is free -> promote queued mission to MATCHED
                    await db.query(
                        "UPDATE orders SET fulfiller_id = $1, status = 'MATCHED', matched_at = CURRENT_TIMESTAMP WHERE id = $2",
                        [o.queued_for_fulfiller_id, o.id]
                    );
                    restoredActiveCount++;
                    console.log(`⚡ Order #${o.id} promoted from Queue to MATCHED for Fulfiller #${o.queued_for_fulfiller_id}`);
                } else {
                    // Agent is busy -> ensure status is QUEUED
                    await db.query(
                        "UPDATE orders SET status = 'QUEUED' WHERE id = $1",
                        [o.id]
                    );
                    restoredQueuedCount++;
                    console.log(`⏳ Order #${o.id} set to QUEUED for Fulfiller #${o.queued_for_fulfiller_id}`);
                }
            }
            else if (o.status === 'QUEUED' && o.fulfiller_id) {
                // Fulfiller is assigned on a queued order -> ensure queued_for_fulfiller_id is set
                await db.query(
                    "UPDATE orders SET queued_for_fulfiller_id = $1, fulfiller_id = NULL WHERE id = $2",
                    [o.fulfiller_id, o.id]
                );
                restoredQueuedCount++;
                console.log(`⏳ Order #${o.id} normalized to QUEUED for Fulfiller #${o.fulfiller_id}`);
            }
            else if (!o.fulfiller_id && !o.queued_for_fulfiller_id) {
                searchingCount++;
                console.log(`📡 Order #${o.id} is SEARCHING for available agents (${o.pickup_address})`);
            }
            else if (o.fulfiller_id) {
                restoredActiveCount++;
                console.log(`ℹ️ Order #${o.id} is active (${o.status}) for Fulfiller #${o.fulfiller_id}`);
            }
        }

        console.log(`\n=== RESTORATION SUMMARY ===`);
        console.log(`• Restored Active Missions (MATCHED): ${restoredActiveCount}`);
        console.log(`• Restored Queued Missions (QUEUED): ${restoredQueuedCount}`);
        console.log(`• Unassigned Searching Missions: ${searchingCount}`);
        console.log(`• Total Processed: ${orders.length}`);

    } catch (e) {
        console.error("[Restoration FATAL Error]:", e.message);
    } finally {
        db.pool.end();
    }
}

restoreMissions();
