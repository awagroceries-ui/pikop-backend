const db = require('../config/db');
const dispatchService = require('../services/dispatchService');

/**
 * Activates SCHEDULED orders once their scheduled_at time has arrived.
 */
const processScheduledOrders = async () => {
    console.log('[ScheduledJob] Checking for due scheduled missions...');

    try {
            UPDATE orders
            SET status = 'SEARCHING'
            WHERE id IN (
                SELECT id FROM orders
                WHERE status = 'SCHEDULED'
                  AND scheduled_at <= (CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Lagos' + INTERVAL '30 minutes')
                LIMIT 50
            )
            RETURNING *

        for (const order of dueOrders) {
            console.log(`[ScheduledJob] Activating Mission #${order.id}`);
            const fulfillers = await dispatchService.findNearbyFulfillers(order);
            if (fulfillers.length > 0) {
                await dispatchService.broadcastOffer(order, fulfillers);
            }
        }
    } catch (e) {
        console.error('[ScheduledJob] Execution failed:', e.message);
    }
};

const startScheduledJob = (intervalMinutes = 15) => {
    setInterval(processScheduledOrders, intervalMinutes * 60 * 1000);
};

module.exports = { startScheduledJob };