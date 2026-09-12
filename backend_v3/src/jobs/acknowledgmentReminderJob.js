const db = require('../config/db');
const fcmService = require('../services/fcmService');

/**
 * Periodically reminds receivers to acknowledge missions and alerts senders of timeouts.
 */
const runAcknowledgmentJobs = async () => {
    console.log('[AcknowledgmentJob] Scanning for pending missions...');

    try {
        // 1. Remind Receivers (30 mins - 2 hours)
        const { rows: pendingReminders } = await db.query(`
            SELECT id, recipient_user_id, created_at FROM orders
            WHERE status = 'PENDING_ACKNOWLEDGMENT'
              AND created_at < NOW() - interval '30 minutes'
              AND created_at > NOW() - interval '2 hours'
        `);

        for (const order of pendingReminders) {
            console.log(`[AcknowledgmentJob] Reminding Receiver for Mission #${order.id}`);
            fcmService.sendNotification(
                order.recipient_user_id,
                "Awaits Your Confirmation 📦",
                "You have a pending delivery request. Tap to confirm your address so we can dispatch an agent.",
                { type: "ACKNOWLEDGMENT_REQUEST", order_id: order.id.toString() }
            );
        }

        // 2. Alert Senders of Timeout (2 hours+)
        const { rows: timeouts } = await db.query(`
            SELECT id, user_id FROM orders
            WHERE status = 'PENDING_ACKNOWLEDGMENT'
              AND created_at < NOW() - interval '2 hours'
              AND created_at > NOW() - interval '4 hours' -- Don't alert for ancient orders
        `);

        for (const order of timeouts) {
            console.log(`[AcknowledgmentJob] Timeout reached for Mission #${order.id}. Alerting Sender.`);
            fcmService.sendNotification(
                order.user_id,
                "Receiver Not Responding ⚠️",
                "Your recipient hasn't acknowledged the delivery request. Tap to choose how to proceed.",
                { type: "ACKNOWLEDGMENT_TIMEOUT", order_id: order.id.toString() }
            );
        }

    } catch (e) {
        console.error('[AcknowledgmentJob] Execution failed:', e.message);
    }
};

const startAcknowledgmentJob = (intervalMinutes = 15) => {
    setInterval(runAcknowledgmentJobs, intervalMinutes * 60 * 1000);
};

module.exports = { startAcknowledgmentJob };
