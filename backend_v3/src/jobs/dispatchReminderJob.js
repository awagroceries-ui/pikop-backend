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
            const ageMinutes = (Date.now() - new Date(order.created_at).getTime()) / 60000;
            const includeFleets = ageMinutes >= 3.0; // Overflow window

            console.log(`[ReminderJob] Nudging Mission #${order.id} (Age: ${ageMinutes.toFixed(1)}m, Fleets: ${includeFleets})`);
            const fulfillers = await dispatchService.findNearbyFulfillers(order, null, includeFleets);
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
