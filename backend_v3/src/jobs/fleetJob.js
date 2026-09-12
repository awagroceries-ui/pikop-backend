const db = require('../config/db');

/**
 * Periodically audits fulfiller performance and updates tiers (v3.9.8).
 * Logic: Analyzes the last 30 days of activity.
 */
const runFleetAudit = async () => {
    console.log('[FleetJob] Starting daily performance audit...');

    try {
        // 1. Fetch all active fulfillers
        const { rows: agents } = await db.query("SELECT id, tier, rating_avg, rating_count FROM fulfillers WHERE status = 'active'");

        for (const agent of agents) {
            // 2. Calculate 30-day stats
            const statsRes = await db.query(`
                SELECT
                    COUNT(*) FILTER (WHERE status = 'DELIVERED') as completed,
                    COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled,
                    COALESCE(SUM(total_fare * 0.75) FILTER (WHERE status = 'DELIVERED'), 0) as earnings
                FROM orders
                WHERE fulfiller_id = $1 AND created_at >= NOW() - interval '30 days'
            `, [agent.id]);

            const stats = statsRes.rows[0];
            const total = parseInt(stats.completed) + parseInt(stats.cancelled);
            const completionRate = total > 0 ? (parseInt(stats.completed) / total) : 1.0;
            const earnings = parseFloat(stats.earnings);
            const rating = parseFloat(agent.rating_avg);

            let newTier = 'Bronze'; // Default for active verified
            let isFlagged = false;
            let flagReason = null;

            // 3. Tier Logic
            if (stats.completed >= 100 && completionRate >= 0.95 && rating >= 4.7) {
                newTier = 'Gold';
            } else if (stats.completed >= 30 && completionRate >= 0.90 && rating >= 4.3) {
                newTier = 'Silver';
            }

            // 4. Flagging Logic (Risk Management)
            if (total >= 10 && completionRate < 0.60) {
                isFlagged = true;
                flagReason = `Low completion rate (${Math.round(completionRate * 100)}%) in last 30 days.`;
            } else if (agent.rating_count >= 5 && rating < 3.0) {
                isFlagged = true;
                flagReason = `Low average rating (${rating}) based on ${agent.rating_count} feedbacks.`;
            }

            // 5. Update Record
            await db.query(`
                UPDATE fulfillers
                SET tier = $1,
                    is_flagged = $2,
                    flag_reason = $3,
                    last_tier_audit_at = CURRENT_TIMESTAMP
                WHERE id = $4
            `, [newTier, isFlagged, flagReason, agent.id]);

            if (agent.tier !== newTier) {
                console.log(`[FleetJob] Agent ${agent.id} promoted/demoted: ${agent.tier} -> ${newTier}`);
            }
        }

        console.log(`[FleetJob] Audit complete. Processed ${agents.length} agents.`);

    } catch (e) {
        console.error('[FleetJob] Audit failed:', e.message);
    }
};

const startFleetJob = (intervalHours = 24) => {
    // Run once on startup
    runFleetAudit();
    // Then every X hours
    setInterval(runFleetAudit, intervalHours * 60 * 60 * 1000);
};

module.exports = { startFleetJob };
