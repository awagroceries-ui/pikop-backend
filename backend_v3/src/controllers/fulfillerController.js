/**
 * Updates fulfiller profile (Class, Mobility, Vehicle).
 */
const updateFulfillerProfile = async (req, res) => {
    const userId = req.user.id;
    const { primary_class, mobility_type, vehicle_details, full_name, phone } = req.body;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Update Core User data if provided
        if (full_name || phone) {
            await client.query(
                "UPDATE users SET full_name = COALESCE($1, full_name), phone = COALESCE($2, phone) WHERE id = $3",
                [full_name, phone, userId]
            );
        }

        // 2. Update Fulfiller table
        const fulfillerRes = await client.query(
            `UPDATE fulfillers
             SET primary_class = COALESCE($1, primary_class),
                 mobility_type = COALESCE($2, mobility_type),
                 registration_number = COALESCE($3, registration_number),
                 make = COALESCE($4, make),
                 model = COALESCE($5, model),
                 color = COALESCE($6, color)
             WHERE user_id = $7
             RETURNING id`,
            [
                primary_class,
                mobility_type,
                vehicle_details?.registration_number,
                vehicle_details?.make,
                vehicle_details?.model,
                vehicle_details?.color,
                userId
            ]
        );

        if (fulfillerRes.rows.length === 0) {
            // Create fulfiller record if it somehow doesn't exist but user is FULFILLER role
            const userRes = await client.query("SELECT full_name, email, phone FROM users WHERE id = $1", [userId]);
            const u = userRes.rows[0];
            await client.query(
                `INSERT INTO fulfillers (user_id, full_name, email, phone, primary_class, password_hash)
                 VALUES ($1, $2, $3, $4, $5, 'external_auth')`,
                [userId, u.full_name, u.email, u.phone, primary_class || 'rider']
            );
        }

        await client.query('COMMIT');
        res.status(200).json({ success: true, message: 'Profile updated' });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Fulfiller] Update Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

module.exports = {
  startIdentityVerification,
  updateFulfillerProfile,
  verifyVehiclePlate,
