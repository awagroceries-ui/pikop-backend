const db = require('../config/db');

/**
 * Validates and applies a coupon code to a quote.
 */
const validateCoupon = async (req, res) => {
    const { code, amount } = req.body;

    try {
        const { rows } = await db.query(
            "SELECT * FROM coupons WHERE code = $1 AND is_active = true AND (expiry_at IS NULL OR expiry_at > NOW())",
            [code.toUpperCase()]
        );

        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: 'Invalid or expired coupon code' });
        }

        const coupon = rows[0];
        const numericAmount = parseFloat(amount);

        if (numericAmount < parseFloat(coupon.min_order_amount)) {
            return res.status(400).json({
                success: false,
                message: `Minimum order of ₦${coupon.min_order_amount} required for this coupon.`
            });
        }

        let discount = 0;
        if (coupon.discount_type === 'FIXED') {
            discount = parseFloat(coupon.discount_value);
        } else {
            discount = numericAmount * (parseFloat(coupon.discount_value) / 100);
            if (coupon.max_discount_amount && discount > parseFloat(coupon.max_discount_amount)) {
                discount = parseFloat(coupon.max_discount_amount);
            }
        }

        res.status(200).json({
            success: true,
            data: {
                code: coupon.code,
                discount_amount: discount,
                final_amount: Math.max(0, numericAmount - discount)
            }
        });

    } catch (error) {
        throw error;
    }
};

/**
 * Returns user referral and loyalty stats.
 */
const getMyGrowthStats = async (req, res) => {
    const userId = req.user.id;

    try {
        const userRes = await db.query("SELECT referral_code FROM users WHERE id = $1", [userId]);
        const pointRes = await db.query("SELECT SUM(points) as total FROM loyalty_ledger WHERE user_id = $1", [userId]);
        const referralRes = await db.query("SELECT COUNT(*) FROM referrals WHERE referrer_id = $1", [userId]);

        res.status(200).json({
            success: true,
            data: {
                referral_code: userRes.rows[0].referral_code,
                total_points: parseInt(pointRes.rows[0].total || 0),
                referral_count: parseInt(referralRes.rows[0].count)
            }
        });
    } catch (error) {
        throw error;
    }
};

module.exports = {
    validateCoupon,
    getMyGrowthStats
};
