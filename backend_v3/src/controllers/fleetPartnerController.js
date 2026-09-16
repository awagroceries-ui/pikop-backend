const db = require('../config/db');
const crypto = require('crypto');

/**
 * Stage 2: Business Verification for Fleet Partners.
 */
const setupFleetProfile = async (req, res) => {
    const userId = req.user.id;
    const { business_name, cac_number, address, fleet_size, vehicle_types, cities } = req.body;

    try {
        const { rows } = await db.query(
            `INSERT INTO fleet_partners (user_id, business_name, cac_number, business_address, fleet_size_estimate, vehicle_types, operating_cities)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             ON CONFLICT (user_id) DO UPDATE SET
                business_name = EXCLUDED.business_name,
                cac_number = EXCLUDED.cac_number,
                business_address = EXCLUDED.business_address,
                fleet_size_estimate = EXCLUDED.fleet_size_estimate,
                vehicle_types = EXCLUDED.vehicle_types,
                operating_cities = EXCLUDED.operating_cities
             RETURNING id, status`,
            [userId, business_name, cac_number, address, fleet_size, vehicle_types, cities]
        );

        res.status(201).json({
            success: true,
            message: 'Fleet partnership application submitted.',
            data: rows[0]
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns dashboard data for a Fleet Partner.
 */
const getFleetDashboard = async (req, res) => {
    const userId = req.user.id;

    try {
        // 1. Get Fleet Partner Profile
        const fleetRes = await db.query("SELECT * FROM fleet_partners WHERE user_id = $1", [userId]);
        if (fleetRes.rows.length === 0) return res.status(404).json({ success: false, message: 'Fleet profile not found' });
        const fleet = fleetRes.rows[0];

        // 2. Get Linked Fulfillers
        const fulfillersRes = await db.query(`
            SELECT f.id, f.full_name, f.primary_class, f.kyc_status, f.online_status, f.rating_avg,
                   (SELECT COUNT(*) FROM orders WHERE fulfiller_id = f.id AND status = 'DELIVERED') as completed_missions
            FROM fulfillers f
            WHERE f.fleet_partner_id = $1
        `, [fleet.id]);

        // 3. Get Aggregate Stats
        const statsRes = await db.query(`
            SELECT
                COUNT(*) as total_active_missions,
                SUM(total_fare) as total_fleet_volume
            FROM orders
            WHERE fulfiller_id IN (SELECT id FROM fulfillers WHERE fleet_partner_id = $1)
            AND created_at >= NOW() - interval '30 days'
        `, [fleet.id]);

        res.status(200).json({
            success: true,
            data: {
                profile: fleet,
                fulfillers: fulfillersRes.rows,
                stats: statsRes.rows[0]
            }
        });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Generates or retrieves an invite code for the fleet.
 */
const getInviteCode = async (req, res) => {
    const userId = req.user.id;

    try {
        const fleetRes = await db.query("SELECT id FROM fleet_partners WHERE user_id = $1", [userId]);
        if (fleetRes.rows.length === 0) return res.status(403).json({ success: false, message: 'Fleet profile required' });
        const fleetId = fleetRes.rows[0].id;

        const { rows } = await db.query("SELECT invite_code FROM fleet_partner_invites WHERE fleet_partner_id = $1 AND is_active = true LIMIT 1", [fleetId]);

        if (rows.length > 0) {
            return res.status(200).json({ success: true, invite_code: rows[0].invite_code });
        }

        const newCode = `FP-${crypto.randomBytes(3).toString('hex').toUpperCase()}`;
        await db.query("INSERT INTO fleet_partner_invites (fleet_partner_id, invite_code) VALUES ($1, $2)", [fleetId, newCode]);

        res.status(201).json({ success: true, invite_code: newCode });
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

module.exports = {
    setupFleetProfile,
    getFleetDashboard,
    getInviteCode
};
