const db = require('../config/db');
const dispatchService = require('../services/dispatchService');

/**
 * Periodically re-broadcasts active 'SEARCHING' orders to nearby fulfillers.
 * This acts as a reminder for unaccepted missions.
 */
const runDispatchReminders = async () => {
    console.log('[ReminderJob] Checking for unaccepted missions...');

    try {
        // Find missions waiting for a fulfiller for more than 2 minutes
        const { rows: pendingOrders } = await db.query(`
            SELECT * FROM orders
            WHERE status IN ('SEARCHING', 'PAYMENT_CAPTURED')
              AND fulfiller_id IS NULL
              AND created_at < NOW() - interval '2 minutes'
              AND created_at > NOW() - interval '30 minutes' -- Don't nudge ancient orders
        `);

        for (const order of pendingOrders) {
            console.log(`[ReminderJob] Nudging Mission #${order.id}`);
            const fulfillers = await dispatchService.findNearbyFulfillers(order);
            if (fulfillers.length > 0) {
                await dispatchService.broadcastOffer(order, fulfillers);
            }
        }
    } catch (e) {
        console.error('[ReminderJob] Execution failed:', e.message);
    }
};

const startReminderJob = (intervalMinutes = 3) => {
    setInterval(runDispatchReminders, intervalMinutes * 60 * 1000);
};

module.exports = { startReminderJob };
