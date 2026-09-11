const db = require('../config/db');
const diditService = require('../services/diditService');
const kycService = require('../services/kycService');
const paystackService = require('../services/paystackService');

const axios = require('axios');

/**
 * Initializes a KYC session.
 */
const startIdentityVerification = async (req, res) => {
  const userId = req.user.id;
  const { provider = process.env.PRIMARY_KYC_PROVIDER || 'prembly' } = req.body;
  const normalizedProvider = provider.toLowerCase();

  console.log(`[KYC] User ${userId} requested verification via: ${normalizedProvider}`);
  console.log(`[KYC] Request Body:`, JSON.stringify(req.body));

  try {
    // 1. Fetch user info for initiation
    const userRes = await db.query("SELECT full_name, email FROM users WHERE id = $1", [userId]);
    if (userRes.rows.length === 0) return res.status(404).json({ success: false, message: 'User not found' });
    const user = userRes.rows[0];

    // Standardizing on v3 abstraction
    if (normalizedProvider === 'didit') {
        const session = await diditService.createSession(userId);
        await db.query(
          "UPDATE fulfillers SET didit_session_id = $1, didit_verification_status = 'pending' WHERE user_id = $2",
          [session.session_id, userId]
        );
        return res.status(200).json({ success: true, data: { url: session.url, token: session.session_token, session_id: session.session_id } });
    }

    // NEW PREMBLY SDK FLOW (Initiate Session)
    if (provider === 'prembly') {
        const nameParts = (user.full_name || 'Pikop User').split(' ');
        const firstName = nameParts[0];
        const lastName = nameParts.length > 1 ? nameParts.slice(1).join(' ') : 'User';

        const payload = {
            first_name: firstName,
            last_name: lastName,
            email: user.email,
            customer_reference: `pikop_kyc_${userId}`,
            widget_id: process.env.PREMBLY_CONFIG_ID || '2183d331-33bd-4568-a67f-c21ffab5e274',
            widget_key: process.env.PREMBLY_WIDGET_ID || 'wdgt_02c17a8d92e54c659279db8cdf5839a2'
        };

        console.log('[Prembly] Initiating session with DOCUMENTED mapping...');
        console.log(`[Prembly] Payload:`, JSON.stringify(payload));

        try {
            const response = await axios.post('https://backend.prembly.com/api/v1/checker-widget/sdk/sessions/initiate/', payload, {
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'x-api-key': process.env.PREMBLY_SECRET_KEY
                },
                timeout: 10000
            });

            if (response.data.status) {
                const sessionId = response.data.data.session.session_id;
                const verificationUrl = `https://sdk-live.prembly.com/?session=${sessionId}`;

                // Store session for tracking (idempotent)
                await db.query(
                    "UPDATE fulfillers SET didit_session_id = $1, didit_verification_status = 'pending' WHERE user_id = $2",
                    [sessionId, userId]
                );

                return res.status(200).json({
                    success: true,
                    data: {
                        url: verificationUrl,
                        session_id: sessionId,
                        session_token: sessionId // Alias
                    }
                });
            } else {
                console.error('[Prembly] Initiation failed:', response.data.message);
                return res.status(400).json({ success: false, message: response.data.message });
            }
        } catch (apiError) {
            console.error('[Prembly] API Error:', apiError.response?.data || apiError.message);
            return res.status(502).json({ success: false, message: 'Could not connect to Prembly' });
        }
    }

    // Default Fallback
    res.status(200).json({
        success: true,
        message: 'KYC initialized via abstraction layer',
        provider
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * Updates fulfiller profile (Class, Mobility, Vehicle, Personal).
 */
const updateFulfillerProfile = async (req, res) => {
    const userId = req.user.id;
    const {
        primary_class, mobility_type, vehicle_details,
        full_name, phone, gender, date_of_birth, home_address,
        bank_name, account_number, bank_code, account_name
    } = req.body;

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Fetch current user core info (email is needed for fulfiller insert if not exists)
        const userRes = await client.query("SELECT full_name, email, phone FROM users WHERE id = $1", [userId]);
        if (userRes.rows.length === 0) throw new Error('User not found');
        const u = userRes.rows[0];

        // 2. Update Core User data if provided
        if (full_name || phone) {
            await client.query(
                "UPDATE users SET full_name = COALESCE($1, full_name), phone = COALESCE($2, phone) WHERE id = $3",
                [full_name, phone, userId]
            );
        }

        // 3. Cleanup: Remove any fulfiller records with same email/phone but different user_id
        // Using IS DISTINCT FROM to handle NULL cases reliably in Postgres.
        await client.query(
            "DELETE FROM fulfillers WHERE (email = $1 OR phone = $2) AND (user_id IS DISTINCT FROM $3)",
            [u.email, phone || u.phone, userId]
        );

        // 4. UPSERT into Fulfiller table (Eliminates 'duplicate key' errors)
        await client.query(
            `INSERT INTO fulfillers (
                user_id, full_name, email, phone, primary_class, password_hash,
                mobility_type, registration_number, make, model, color,
                bank_name, account_number, bank_code, account_name,
                gender, date_of_birth, home_address
             ) VALUES ($1, $2, $3, $4, $5, 'external_auth', $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17)
             ON CONFLICT (user_id) DO UPDATE SET
                primary_class = COALESCE(EXCLUDED.primary_class, fulfillers.primary_class),
                mobility_type = COALESCE(EXCLUDED.mobility_type, fulfillers.mobility_type),
                registration_number = COALESCE(EXCLUDED.registration_number, fulfillers.registration_number),
                make = COALESCE(EXCLUDED.make, fulfillers.make),
                model = COALESCE(EXCLUDED.model, fulfillers.model),
                color = COALESCE(EXCLUDED.color, fulfillers.color),
                bank_name = COALESCE(EXCLUDED.bank_name, fulfillers.bank_name),
                account_number = COALESCE(EXCLUDED.account_number, fulfillers.account_number),
                bank_code = COALESCE(EXCLUDED.bank_code, fulfillers.bank_code),
                account_name = COALESCE(EXCLUDED.account_name, fulfillers.account_name),
                gender = COALESCE(EXCLUDED.gender, fulfillers.gender),
                date_of_birth = COALESCE(EXCLUDED.date_of_birth, fulfillers.date_of_birth),
                home_address = COALESCE(EXCLUDED.home_address, fulfillers.home_address)`,
            [
                userId,
                full_name || u.full_name,
                u.email,
                phone || u.phone,
                primary_class || 'rider',
                mobility_type,
                vehicle_details?.registration_number,
                vehicle_details?.make,
                vehicle_details?.model,
                vehicle_details?.color,
                bank_name,
                account_number,
                bank_code,
                account_name,
                gender,
                date_of_birth,
                home_address
            ]
        );

        await client.query('COMMIT');
        res.status(200).json({ success: true, message: 'Profile updated successfully' });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('[Fulfiller] Update Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    } finally {
        client.release();
    }
};

/**
 * Verifies vehicle plate number for Drivers.
 */
const verifyVehiclePlate = async (req, res) => {
    const userId = req.user.id;
    const { plate_number } = req.body;

    if (!plate_number) return res.status(400).json({ success: false, message: 'Plate number required' });

    try {
        const { rows: fulfiller } = await db.query("SELECT id, primary_class FROM fulfillers WHERE user_id = $1", [userId]);
        if (fulfiller.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller not found' });

        if (fulfiller[0].primary_class !== 'DRIVER' && fulfiller[0].primary_class !== 'driver') {
            return res.status(403).json({ success: false, message: 'Plate verification only required for Driver tier' });
        }

        const result = await kycService.executeCheck('verifyVehiclePlate', plate_number);

        // Store result linked to fulfiller + vehicle logic (simplified for v3 core)
        await db.query(
            "UPDATE fulfillers SET registration_number = $1, kyc_status = $2 WHERE id = $3",
            [plate_number, result.status === 'SUCCESS' ? 'VERIFIED' : 'FAILED', fulfiller[0].id]
        );

        res.status(200).json(result);
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Handles signed decision webhooks from Didit (Milestone 5).
 */
const handleDiditWebhook = async (req, res) => {
  const rawBody = JSON.stringify(req.body);
  const signature = req.headers['x-signature-v2'];
  const timestamp = req.headers['x-timestamp'];

  if (!diditService.verifyWebhook(rawBody, signature, timestamp)) {
    return res.status(401).send('Invalid signature');
  }

  const { vendor_data, status } = req.body;
  const userId = vendor_data;

  // Map Didit status to Pikop v3 status
  const statusMap = { 'Approved': 'approved', 'Declined': 'declined' };
  const verifiedStatus = statusMap[status] || 'pending';

  try {
    await db.query(
      "UPDATE fulfillers SET didit_verification_status = $1, didit_verified_at = CURRENT_TIMESTAMP WHERE user_id = $2",
      [verifiedStatus, userId]
    );
    console.log(`[Didit Webhook] User ${userId} updated to ${verifiedStatus}`);
    res.status(200).send('OK');
  } catch (error) {
    console.error('[Didit Webhook] Error:', error.message);
    res.status(500).send('Database error');
  }
};

/**
 * Uploads a document (Logistics Milestone 2, KYCDocument).
 */
const uploadDocument = async (req, res) => {
  const userId = req.user.id;
  const { doc_type, expiry_date } = req.body;
  if (!req.file) return res.status(400).json({ success: false, message: 'File required' });

  // Milestone 12: Preparing for S3/B2 (currently using local path)
  const fileUrl = `/uploads/${req.file.filename}`;

  try {
    const { rows: fulfiller } = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    if (fulfiller.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller record missing' });

    await db.query(
      `INSERT INTO kyc_documents (fulfiller_id, doc_type, file_url, status, expiry_date)
       VALUES ($1, $2, $3, 'PENDING', $4)`,
      [fulfiller[0].id, doc_type, fileUrl, expiry_date]
    );

    // Emit real-time alert for admin (v3.5.1)
    try {
        const socketService = require('../services/socketService');
        socketService.getIO().emit("new_kyc_alert", { fulfillerId: fulfiller[0].id, type: doc_type });
    } catch (e) {}

    res.status(201).json({ success: true, message: 'Document uploaded' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * Updates fulfiller online status and current GPS location.
 */
const updateStatus = async (req, res) => {
  const { online_status, lat, lng, current_state } = req.body;
  const userId = req.user.id;

  try {
    const query = `
      UPDATE fulfillers
      SET online_status = $1,
          current_location = ST_SetSRID(ST_MakePoint($2, $3), 4326),
          current_state = COALESCE($5, current_state),
          last_ping_at = CURRENT_TIMESTAMP,
          last_active_at = CURRENT_TIMESTAMP
      WHERE user_id = $4
      RETURNING id, online_status
    `;

    const { rows } = await db.query(query, [online_status || 'OFFLINE', lng || 0, lat || 0, userId, current_state]);

    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller profile not found' });

    console.log(`[Fleet] Fulfiller ${rows[0].id} is now ${rows[0].online_status}`);

    res.status(200).json({
      success: true,
      data: { status: rows[0].online_status }
    });
  } catch (error) {
    throw error;
  }
};

/**
 * Fetches the fulfiller's own profile and stats.
 */
const getProfile = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
        "SELECT * FROM fulfillers WHERE user_id = $1",
        [userId]
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Profile not found' });

    res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

/**
 * Fetches available delivery offers for online fulfillers.
 */
const getAvailableOffers = async (req, res) => {
  const userId = req.user.id;

  try {
    const { rows: fulfiller } = await db.query(
      "SELECT id, online_status, primary_class FROM fulfillers WHERE user_id = $1",
      [userId]
    );

    if (fulfiller.length === 0) {
      return res.status(404).json({ success: false, message: 'Fulfiller not found' });
    }

    if (fulfiller[0].online_status !== 'ONLINE') {
      return res.status(200).json([]);
    }

    const fulfillerId = fulfiller[0].id;

    // Fetch active SEARCHING or PAYMENT_CAPTURED orders (unassigned or queued for this fulfiller)
    // CRITICAL: Filter by Same State, 20km Radius, and exclude stale fulfillers.
    // ADDED: Size-based Class Eligibility and Zone-based Okada restrictions.
    // REFINED: Case-insensitive state matching for higher visibility.
    const { rows } = await db.query(
      `SELECT o.id, o.pickup_address, o.delivery_address, o.total_fare, o.item_photo_url, o.created_at,
       o.collect_on_delivery_amount,
       ST_Distance(f.current_location::geography, o.pickup_location::geography) / 1000 as distance_km,
       ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng,
       ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng
       FROM orders o
       CROSS JOIN fulfillers f
       LEFT JOIN zones z ON ST_Intersects(o.pickup_location::geometry, z.boundary::geometry)
       WHERE f.id = $1
       AND o.status IN ('SEARCHING', 'PAYMENT_CAPTURED')
       AND (o.fulfiller_id IS NULL OR o.queued_for_fulfiller_id = $1)
       AND o.pickup_state ILIKE $2 -- Resilient state matching
       AND ST_DWithin(f.current_location::geography, o.pickup_location::geography, 20000)
       AND f.last_ping_at > NOW() - interval '30 minutes'

       -- Class-based Eligibility (from Size Tier)
       AND f.primary_class = ANY(o.required_fulfiller_classes)

       -- Zone-based Restrictions (e.g. No Okada in certain Lagos LGAs)
       AND (z.id IS NULL OR f.primary_class = ANY(z.allowed_fulfiller_classes))

       ORDER BY o.created_at DESC
       LIMIT 20`,
      [fulfillerId, `%${(fulfiller[0].current_state || '').split(' ')[0]}%`]
    );

    res.status(200).json(rows);
  } catch (error) {
    console.error('[Offers] Fetch error:', error.message);
    res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * Handles fulfiller avatar updates.
 */
const uploadProfilePhoto = async (req, res) => {
    const userId = req.user.id;
    if (!req.file) return res.status(400).json({ success: false, message: 'No file uploaded' });

    const photoUrl = `/uploads/${req.file.filename}`;

    try {
        // Update both tables to keep profile in sync across schemas
        await db.query("UPDATE users SET profile_photo_url = $1 WHERE id = $2", [photoUrl, userId]);
        await db.query("UPDATE fulfillers SET profile_photo_url = $1 WHERE user_id = $2", [photoUrl, userId]);

        res.status(200).json({ success: true, url: photoUrl });
    } catch (error) {
        throw error;
    }
};

/**
 * Submits the application for final admin review.
 */
const submitApplication = async (req, res) => {
    const userId = req.user.id;

    try {
        const { rows } = await db.query(
            "UPDATE fulfillers SET kyc_status = 'PENDING_REVIEW' WHERE user_id = $1 RETURNING id, kyc_status",
            [userId]
        );

        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Profile not found' });

        res.status(200).json({ success: true, message: 'Application submitted successfully', data: rows[0] });
    } catch (error) {
        console.error('[KYC Submit] Error:', error.message);
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Returns a list of Nigerian banks.
 */
const getBanks = async (req, res) => {
    try {
        const banks = await paystackService.getBanks();
        res.status(200).json(banks);
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Resolves account number.
 */
const resolveAccount = async (req, res) => {
    const { account_number, bank_code } = req.body;
    try {
        const result = await paystackService.resolveAccount(account_number, bank_code);
        res.status(200).json(result);
    } catch (error) {
        res.status(400).json({ success: false, message: error.message });
    }
};

module.exports = {
  startIdentityVerification,
  updateFulfillerProfile,
  verifyVehiclePlate,
  handleDiditWebhook,
  uploadDocument,
  updateStatus,
  getProfile,
  uploadProfilePhoto,
  getAvailableOffers,
  submitApplication,
  getBanks,
  resolveAccount
};
