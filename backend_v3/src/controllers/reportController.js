const db = require('../config/db');

/**
 * Aggregates performance metrics across the three core modules.
 */
const getModulePerformance = async (req, res) => {
    const days = parseInt(req.query.days) || 30;

    try {
        const query = `
            SELECT
                order_type,
                COUNT(*) as volume,
                COALESCE(SUM(total_fare), 0) as gmv,
                COUNT(*) FILTER (WHERE status = 'DELIVERED') as completed
            FROM orders
            WHERE created_at >= NOW() - INTERVAL '$1 days'
            GROUP BY order_type
        `.replace('$1', days); // Simplified for v3 alpha

        const { rows } = await db.query(query);

        // Convert to a friendlier format for the UI
        const stats = {
            pickup_delivery: { volume: 0, gmv: 0, completed: 0 },
            marketplace: { volume: 0, gmv: 0, completed: 0 },
            food: { volume: 0, gmv: 0, completed: 0 }
        };

        rows.forEach(r => {
            if (stats[r.order_type]) {
                stats[r.order_type] = {
                    volume: parseInt(r.volume),
                    gmv: parseFloat(r.gmv),
                    completed: parseInt(r.completed)
                };
            }
        });

        res.status(200).json({ success: true, data: stats });
    } catch (error) {
        throw error;
    }
};

/**
 * Calculates user engagement metrics (DAU/MAU).
 */
const getUserAnalytics = async (req, res) => {
    try {
        const dau = await db.query("SELECT COUNT(*) FROM users WHERE last_active_at >= NOW() - INTERVAL '24 hours'");
        const mau = await db.query("SELECT COUNT(*) FROM users WHERE last_active_at >= NOW() - INTERVAL '30 days'");

        const fulfillerDau = await db.query("SELECT COUNT(*) FROM fulfillers WHERE last_active_at >= NOW() - INTERVAL '24 hours'");

        res.status(200).json({
            success: true,
            data: {
                dau: parseInt(dau.rows[0].count),
                mau: parseInt(mau.rows[0].count),
                active_fulfillers: parseInt(fulfillerDau.rows[0].count)
            }
        });
    } catch (error) {
        throw error;
    }
};

/**
 * Renders the advanced reports view.
 */
const renderReports = async (req, res) => {
    try {
        // Fetch snapshot for initial render
        const moduleStats = await db.query(`
            SELECT order_type, COUNT(*) as count, SUM(total_fare) as gmv
            FROM orders GROUP BY order_type
        `);

        const cityStats = await db.query(`
            SELECT city, COUNT(*) as count FROM vendors GROUP BY city
            UNION
            SELECT city, COUNT(*) as count FROM kitchens GROUP BY city
        `);

        res.render('reports', {
            moduleStats: moduleStats.rows,
            cityStats: cityStats.rows
        });
    } catch (error) {
        res.status(500).render('error', { message: error.message });
    }
};

/**
 * Generates a CSV export of all missions.
 */
const exportMissions = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT id, order_type, status, total_fare, pickup_address, delivery_address, created_at
            FROM orders ORDER BY created_at DESC
        `);

        let csv = 'Mission ID,Type,Status,Fare (NGN),Pickup,Delivery,Date\n';
        rows.forEach(r => {
            csv += `${r.id},${r.order_type},${r.status},${r.total_fare},"${r.pickup_address.replace(/"/g, '""')}","${r.delivery_address.replace(/"/g, '""')}",${r.created_at.toISOString()}\n`;
        });

        res.header('Content-Type', 'text/csv');
        res.attachment(`pikop_missions_${Date.now()}.csv`);
        return res.send(csv);
    } catch (error) {
        res.status(500).send('Export failed: ' + error.message);
    }
};

module.exports = {
    getModulePerformance,
    getUserAnalytics,
    renderReports,
    exportMissions
};
