const db = require('../config/db');
const walletService = require('../services/walletService');

/**
 * Periodically finds and releases expired escrow orders.
 */
const runAutoRelease = async () => {
    console.log(`[Job] EscrowWorker: Checking for expired grace periods...`);

    try {
        const { rows: expiredOrders } = await db.query(`
            SELECT id FROM orders
            WHERE status = 'DELIVERED_PENDING_CONFIRMATION'
            AND escrow_status = 'held'
            AND grace_period_expires_at <= CURRENT_TIMESTAMP
            FOR UPDATE SKIP LOCKED
        `);

        if (expiredOrders.length === 0) {
            console.log(`[Job] No expired orders found.`);
            return;
        }

        console.log(`[Job] Found ${expiredOrders.length} orders to auto-release.`);

        for (const order of expiredOrders) {
            try {
                await walletService.releaseEscrow(order.id);
                console.log(`[Job] Auto-released Order #${order.id}`);
            } catch (err) {
                console.error(`[Job] Failed to release Order #${order.id}:`, err.message);
            }
        }
    } catch (error) {
        console.error(`[Job] EscrowWorker Fatal Error:`, error.message);
    }
};

// Simple polling loop for basic v3 implementation
const startEscrowWorker = (intervalMinutes = 30) => {
    setInterval(runAutoRelease, intervalMinutes * 60 * 1000);
    // Run once immediately on start
    runAutoRelease();
};

module.exports = { startEscrowWorker };
