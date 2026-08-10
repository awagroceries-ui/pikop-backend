const db = require('../config/db');
const { getFulfillerPublicProfile } = require('./orderController');

/**
 * Returns safe tracking data for the public link.
 */
const getPublicTracking = async (req, res) => {
    const { token } = req.params;

    try {
        const { rows } = await db.query(`
            SELECT id, status, fulfiller_id, pickup_display_summary, delivery_display_summary,
                   ST_Y(pickup_location::geometry) as pickup_lat, ST_X(pickup_location::geometry) as pickup_lng,
                   ST_Y(delivery_location::geometry) as delivery_lat, ST_X(delivery_location::geometry) as delivery_lng
            FROM orders WHERE tracking_token = $1`, [token]);

        if (rows.length === 0) return res.status(404).render('public_tracking', { error: 'Tracking link invalid or expired.' });

        const order = rows[0];
        const profile = await getFulfillerPublicProfile(order.fulfiller_id);

        // Fetch Live Location if active
        let liveLocation = null;
        if (['EN_ROUTE_TO_PICKUP', 'ARRIVED_AT_PICKUP', 'PICKED_UP', 'EN_ROUTE_TO_DELIVERY', 'ARRIVED_AT_DELIVERY'].includes(order.status)) {
            const fRes = await db.query("SELECT ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng FROM fulfillers WHERE id = $1", [order.fulfiller_id]);
            if (fRes.rows.length > 0) liveLocation = fRes.rows[0];
        }

        const data = {
            status: order.status,
            fulfiller: profile,
            live_location: liveLocation,
            pickup_zone: order.pickup_display_summary,
            delivery_zone: order.delivery_display_summary,
            token: token
        };

        res.render('public_tracking', { order: data });
    } catch (error) {
        res.status(500).send('Tracking error');
    }
};

module.exports = {
    getPublicTracking
};
