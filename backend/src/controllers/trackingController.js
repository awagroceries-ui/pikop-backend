const db = require('../config/db');
const { getFulfillerPublicProfile } = require('./orderController');

/**
 * Returns safe tracking data for the public link (Prompt 12).
 */
const getPublicTracking = async (req, res) => {
    const { token } = req.params;

    try {
        const { rows } = await db.query(`
            SELECT id, status, fulfiller_id, pickup_display_summary, delivery_display_summary
            FROM orders WHERE tracking_token = $1`, [token]);

        if (rows.length === 0) return res.status(404).send('Tracking link invalid or expired.');

        const order = rows[0];

        // Static response for finished orders
        if (['DELIVERED', 'CLOSED', 'CANCELLED'].includes(order.status)) {
            return res.render('public_tracking_static', {
                message: order.status === 'DELIVERED' ? 'This delivery has been completed.' : 'This delivery was cancelled.',
                token: token
            });
        }

        const profile = await getFulfillerPublicProfile(order.fulfiller_id);

        // Fetch Live Location if active (Prompt 12, point 1)
        let liveLocation = null;
        const liveWindow = ['EN_ROUTE_TO_PICKUP', 'ARRIVED_AT_PICKUP', 'PICKED_UP', 'EN_ROUTE_TO_DELIVERY', 'ARRIVED_AT_DELIVERY'];
        if (order.fulfiller_id && liveWindow.includes(order.status)) {
            const fRes = await db.query("SELECT ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng FROM fulfillers WHERE id = $1", [order.fulfiller_id]);
            if (fRes.rows.length > 0) liveLocation = fRes.rows[0];
        }

        const data = {
            status: order.status,
            fulfiller: profile,
            live_location: liveLocation,
            pickup_zone: order.pickup_display_summary || 'Masked pickup zone',
            delivery_zone: order.delivery_display_summary || 'Masked delivery zone',
            token: token
        };

        res.render('public_tracking', { order: data });
    } catch (error) {
        console.error("Public Tracking Error:", error);
        res.status(500).send('Tracking error');
    }
};

module.exports = {
    getPublicTracking
};
