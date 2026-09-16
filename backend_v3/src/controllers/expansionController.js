const db = require('../config/db');

/**
 * Adds a user to the expansion waitlist (Nationwide Readiness v4.2).
 */
const joinWaitlist = async (req, res) => {
    const { city_name, state_name, email } = req.body;
    const userId = req.user?.id;

    if (!city_name || !email) {
        return res.status(400).json({ success: false, message: 'City and Email are required.' });
    }

    try {
        await db.query(
            `INSERT INTO expansion_waitlist (user_id, email, city_name, state_name)
             VALUES ($1, $2, $3, $4)
             ON CONFLICT DO NOTHING`,
            [userId || null, email.toLowerCase(), city_name, state_name]
        );

        res.status(201).json({
            success: true,
            message: "We've recorded your interest! We'll notify you when Pikop launches in your area."
        });
    } catch (error) {
        console.error('[Waitlist] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns rules for a specific city.
 */
const getCityRules = async (req, res) => {
    const { name } = req.params;
    try {
        const { rows } = await db.query("SELECT * FROM operating_cities WHERE name ILIKE $1", [`%${name}%`]);
        if (rows.length === 0) {
            return res.status(200).json({ is_live: false, requires_rider_permit: false });
        }
        res.status(200).json({
            is_live: rows[0].is_active,
            requires_rider_permit: rows[0].requires_rider_permit,
            daylight_start: rows[0].daylight_start,
            daylight_end: rows[0].daylight_end
        });
    } catch (error) {
        res.status(500).json({ success: false });
    }
};

module.exports = {
    joinWaitlist,
    getCityRules
};
