const db = require('../config/db');

/**
 * Aggregates platform-wide financial metrics.
 */
const getFinancialOverview = async (req, res) => {
    const { range = 'daily', offset = 0 } = req.query;
    const numericOffset = parseInt(offset);

    try {
        // 1. Define Time Boundaries in WAT (UTC+1)
        let interval;
        let truncate;
        switch (range) {
            case 'weekly': interval = '1 week'; truncate = 'week'; break;
            case 'monthly': interval = '1 month'; truncate = 'month'; break;
            case 'annual': interval = '1 year'; truncate = 'year'; break;
            default: interval = '1 day'; truncate = 'day'; break;
        }

        // Map UI range to valid Postgres interval keywords
        const intervalUnitMap = {
            'daily': 'day',
            'weekly': 'week',
            'monthly': 'month',
            'annual': 'year'
        };
        const unit = intervalUnitMap[range] || 'day';

        const baseTime = `(DATE_TRUNC('${truncate}', NOW() AT TIME ZONE 'Africa/Lagos') - INTERVAL '${numericOffset} ${unit}')`;
        const startTime = `${baseTime}`;
        const endTime = `(${baseTime} + INTERVAL '${interval}')`;

        const prevStartTime = `(${baseTime} - INTERVAL '${interval}')`;
        const prevEndTime = `${baseTime}`;

        // 2. Metrics Queries
        const metricsQuery = (start, end) => `
            SELECT
                COUNT(*) FILTER (WHERE purpose IN ('SETTLEMENT', 'COMMISSION')) as tx_count,
                COALESCE(SUM(amount) FILTER (WHERE purpose = 'COMMISSION'), 0) as commission,
                COALESCE(SUM(amount) FILTER (WHERE purpose = 'SECURE_PAY_FEE'), 0) as platform_fees,
                COALESCE(SUM(amount) FILTER (WHERE purpose = 'SMS_CHARGE'), 0) as sms_earnings,
                COALESCE(SUM(amount) FILTER (WHERE purpose = 'SETTLEMENT' AND wallet_id IN (SELECT id FROM wallets WHERE owner_type = 'USER')), 0) as fulfiller_earnings
            FROM wallet_ledger_entries
            WHERE created_at >= ${start} AND created_at < ${end}
        `;

        const codQuery = (start, end) => `
            SELECT
                COALESCE(SUM(item_price), 0) as gross_volume,
                COALESCE(SUM(item_price) FILTER (WHERE status IN ('DELIVERED', 'RELEASED')), 0) as settled_volume
            FROM orders
            WHERE created_at >= ${start} AND created_at < ${end} AND item_price > 0
        `;

        const [currMetrics, prevMetrics, currCod, prevCod] = await Promise.all([
            db.query(metricsQuery(startTime, endTime)),
            db.query(metricsQuery(prevStartTime, prevEndTime)),
            db.query(codQuery(startTime, endTime)),
            db.query(codQuery(prevStartTime, prevEndTime))
        ]);

        const c = currMetrics.rows[0];
        const p = prevMetrics.rows[0];
        const cc = currCod.rows[0];
        const pc = prevCod.rows[0];

        // 3. Prepare Chart Data (Trend over sub-periods)
        let seriesTrunc;
        let seriesCount;
        switch (range) {
            case 'weekly': seriesTrunc = 'day'; seriesCount = 7; break;
            case 'monthly': seriesTrunc = 'day'; seriesCount = 30; break;
            case 'annual': seriesTrunc = 'month'; seriesCount = 12; break;
            default: seriesTrunc = 'hour'; seriesCount = 24; break;
        }

        const trendQuery = `
            SELECT
                DATE_TRUNC('${seriesTrunc}', created_at AT TIME ZONE 'Africa/Lagos') as period,
                COALESCE(SUM(amount) FILTER (WHERE purpose IN ('COMMISSION', 'SECURE_PAY_FEE', 'SMS_CHARGE')), 0) as earnings
            FROM wallet_ledger_entries
            WHERE created_at >= ${startTime} AND created_at < ${endTime}
            GROUP BY 1 ORDER BY period ASC
        `;
        const trend = await db.query(trendQuery);

        const data = {
            range,
            offset: numericOffset,
            period: { start: startTime, end: endTime },
            summary: {
                totalTransactions: parseInt(c.tx_count),
                platformEarnings: parseFloat(c.commission) + parseFloat(c.platform_fees) + parseFloat(c.sms_earnings),
                platformEarningsBreakdown: {
                    commission: parseFloat(c.commission),
                    fees: parseFloat(c.platform_fees),
                    sms: parseFloat(c.sms_earnings)
                },
                fulfillerEarnings: parseFloat(c.fulfiller_earnings),
                codVolume: {
                    gross: parseFloat(cc.gross_volume),
                    settled: parseFloat(cc.settled_volume)
                }
            },
            comparison: {
                txDelta: calculateDelta(c.tx_count, p.tx_count),
                earningsDelta: calculateDelta(parseFloat(c.commission) + parseFloat(c.platform_fees) + parseFloat(c.sms_earnings), parseFloat(p.commission) + parseFloat(p.platform_fees) + parseFloat(p.sms_earnings)),
                fulfillerDelta: calculateDelta(c.fulfiller_earnings, p.fulfiller_earnings)
            },
            trend: trend.rows
        };

        if (req.headers.accept?.includes('json')) {
            return res.json({ success: true, data });
        }

        res.render('financial_overview', { data });
    } catch (error) {
        console.error('[Financial] Overview Error:', error.message);
        res.status(500).render('error', { message: error.message });
    }
};

const calculateDelta = (curr, prev) => {
    if (!prev || prev == 0) return curr > 0 ? 100 : 0;
    return Math.round(((curr - prev) / prev) * 100);
};

module.exports = {
    getFinancialOverview
};
