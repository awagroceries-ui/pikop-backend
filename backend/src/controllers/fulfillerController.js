const db = require('../config/db');
const dispatchService = require('../services/dispatchService');
const diditService = require('../services/diditService');

/**
 * Internal helper to ensure a fulfiller record exists for a user.
 * Uses ON CONFLICT logic if the user_id has a unique constraint,
 * otherwise performs a safe check-then-insert.
 */
const ensureFulfillerExists = async (userId) => {
  const check = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
  if (check.rows.length > 0) return check.rows[0].id;

  try {
    const result = await db.query(
      "INSERT INTO fulfillers (user_id) VALUES ($1) ON CONFLICT (user_id) DO UPDATE SET user_id = EXCLUDED.user_id RETURNING id",
      [userId]
    );
    return result.rows[0].id;
  } catch (e) {
    // Fallback if unique constraint is named differently or missing
    const retry = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    return retry.rows[0]?.id;
  }
};

/**
 * Initializes a Didit KYC session for the fulfiller.
 */
const startDiditVerification = async (req, res) => {
  const userId = req.user.id;

  try {
    await ensureFulfillerExists(userId);
    const session = await diditService.createSession(userId);

    await db.query(
      "UPDATE fulfillers SET didit_session_id = $1, didit_verification_status = 'pending' WHERE user_id = $2",
      [session.session_id, userId]
    );

    res.status(200).json({
      url: session.url,
      session_token: session.session_token,
      session_id: session.session_id
    });
  } catch (error) {
    console.error('Start Verification Error:', error.message);
    res.status(502).json({ error: error.message });
  }
};

/**
 * Handles signed decision webhooks from Didit.
 */
const handleDiditWebhook = async (req, res) => {
  const raw = JSON.stringify(req.body);
  const sig = req.headers['x-signature-v2'];
  const ts = req.headers['x-timestamp'];

  console.log('[Didit Webhook] Received payload:', raw);

  if (!diditService.verifyWebhookSignature(raw, sig, ts)) {
    console.error('[Didit Webhook] Signature verification failed');
    return res.status(401).send('Invalid signature');
  }

  const event = req.body;
  const userId = event.vendor_data;

  // Didit status can be: Approved, Declined, In Review, Pending, etc.
  const statusMap = {
    'Approved': 'approved',
    'Declined': 'declined',
    'In Review': 'needs_review',
    'approved': 'approved',
    'declined': 'declined'
  };

  const newStatus = statusMap[event.status] || 'pending';
  console.log(`[Didit Webhook] Mapping status "${event.status}" to "${newStatus}" for User ${userId}`);

  try {
    const result = await db.query(
      "UPDATE fulfillers SET didit_verification_status = $1, didit_verified_at = CURRENT_TIMESTAMP WHERE user_id = $2 RETURNING id",
      [newStatus, userId]
    );

    if (result.rows.length === 0) {
      console.warn(`[Didit Webhook] No fulfiller found for User ${userId}`);
    } else {
      console.log(`[Didit Webhook] Updated User ${userId} status to ${newStatus}`);
    }

    res.status(200).send('OK');
  } catch (error) {
    console.error('[Didit Webhook] Database Error:', error.message);
    res.status(500).send('Internal Error');
  }
};

/**
 * Updates fulfiller status and current location.
 */
const updateStatus = async (req, res) => {
  const userId = req.user.id;
  const { online_status, lat, lng } = req.body;

  try {
    const fulfillerId = await ensureFulfillerExists(userId);

    let query = "UPDATE fulfillers SET online_status = $1, last_ping_at = CURRENT_TIMESTAMP";
    const params = [online_status];

    if (lat !== undefined && lng !== undefined) {
      query += ", location = ST_SetSRID(ST_MakePoint($2, $3), 4326)";
      params.push(lng, lat);
    }

    query += " WHERE id = $" + (params.length + 1) + " RETURNING online_status";
    params.push(fulfillerId);

    const { rows } = await db.query(query, params);
    res.status(200).json({ message: 'Status updated', status: rows[0].online_status });
  } catch (error) {
    res.status(500).json({ error: 'Failed to update status' });
  }
};

/**
 * Updates branching profile data (Mobility/Vehicle).
 */
const updateProfile = async (req, res) => {
  const userId = req.user.id;
  const { mobility_type, vehicle_details, primary_class } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    if (mobility_type) {
      await client.query("UPDATE fulfillers SET mobility_type = $1 WHERE user_id = $2", [mobility_type, userId]);
    }

    if (primary_class) {
      await client.query("UPDATE fulfillers SET primary_class = $1 WHERE user_id = $2", [primary_class.toLowerCase(), userId]);
    }

    if (vehicle_details) {
      const { registration_number, make, model, color } = vehicle_details;
      const fRes = await client.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
      const fId = fRes.rows[0].id;

      await client.query(
        `INSERT INTO vehicles (fulfiller_id, registration_number, make, model, color)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (registration_number) DO UPDATE SET make=$3, model=$4, color=$5`,
        [fId, registration_number, make, model, color]
      );
    }

    await client.query('COMMIT');
    res.status(200).json({ message: 'Profile updated' });
  } catch (error) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: error.message });
  } finally {
    client.release();
  }
};

/**
 * Uploads a mandatory live profile photo.
 */
const uploadProfilePhoto = async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Live photo is required' });

  const userId = req.user.id;
  const photoUrl = `/uploads/${req.file.filename}`;

  try {
    await ensureFulfillerExists(userId);
    await db.query("UPDATE fulfillers SET profile_photo_url = $1 WHERE user_id = $2", [photoUrl, userId]);
    res.status(200).json({ message: 'Photo uploaded', url: photoUrl });
  } catch (error) {
    res.status(500).json({ error: 'Failed to save profile photo' });
  }
};

/**
 * Finalizes the application for review.
 */
const submitApplication = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(`
      SELECT f.*, u.role
      FROM fulfillers f
      JOIN users u ON u.id = f.user_id
      WHERE f.user_id = $1`, [userId]);

    const f = rows[0];

    // Validation Gating
    if (!f.profile_photo_url) return res.status(400).json({ error: 'Live profile photo is missing' });
    if (f.didit_verification_status !== 'approved') return res.status(400).json({ error: 'Identity verification not approved yet' });

    // Class specific docs check
    const docsRes = await db.query("SELECT document_type FROM kyc_documents WHERE fulfiller_id = $1", [f.id]);
    const docTypes = docsRes.rows.map(d => d.document_type);

    if (f.primary_class === 'rider' || f.primary_class === 'driver') {
      const v = await db.query("SELECT id FROM vehicles WHERE fulfiller_id = $1", [f.id]);
      if (v.rows.length === 0) return res.status(400).json({ error: 'Vehicle details are missing' });

      const required = f.primary_class === 'rider' ? 'RIDERS_LICENSE' : 'DRIVERS_LICENSE';
      if (!docTypes.includes(required)) return res.status(400).json({ error: `Missing ${required.replace('_', ' ')}` });
    }

    await db.query("UPDATE fulfillers SET kyc_status = 'PENDING_REVIEW' WHERE id = $1", [f.id]);
    res.status(200).json({ message: 'Application submitted for review' });
  } catch (error) {
    res.status(500).json({ error: 'Submission failed' });
  }
};

/**
 * Returns the fulfiller profile status.
 */
const getProfile = async (req, res) => {
  const userId = req.user.id;
  try {
    // Ensure record exists before querying to avoid 404 for new fulfillers
    await ensureFulfillerExists(userId);

    const { rows } = await db.query(`
      SELECT f.id, f.online_status, f.kyc_status, f.didit_verification_status,
             f.mobility_type, f.profile_photo_url, f.tier, f.primary_class,
             v.registration_number, v.make, v.model, v.color
      FROM fulfillers f
      LEFT JOIN vehicles v ON v.fulfiller_id = f.id
      WHERE f.user_id = $1`, [userId]);

    if (rows.length === 0) {
      console.warn(`[Profile] No rows found for User ${userId} after ensureFulfillerExists`);
      return res.status(404).json({ error: 'Fulfiller profile not found' });
    }

    // Ensure critical fields aren't null for the frontend
    const profile = rows[0];
    profile.online_status = profile.online_status || 'OFFLINE';
    profile.kyc_status = profile.kyc_status || 'NOT_STARTED';
    profile.didit_verification_status = profile.didit_verification_status || 'pending';

    res.status(200).json(profile);
  } catch (error) {
    console.error('Get Profile Error:', error.message);
    // Log the full stack in development
    if (process.env.NODE_ENV !== 'production') console.error(error);

    res.status(500).json({
      error: 'Failed to fetch profile',
      detail: error.message
    });
  }
};

/**
 * Returns all orders fulfilled by the authenticated user.
 */
const getFulfillerOrders = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
      `SELECT o.id, o.status, o.total_fare, o.pickup_address, o.delivery_address, o.created_at,
              (o.total_fare * 0.75) as earnings
       FROM orders o
       JOIN fulfillers f ON f.id = o.fulfiller_id
       WHERE f.user_id = $1
       ORDER BY o.created_at DESC`,
      [userId]
    );
    res.status(200).json(rows);
  } catch (error) {
    console.error('Get Fulfiller Orders Error:', error);
    res.status(500).json({ error: 'Failed to fetch your delivery history' });
  }
};

/**
 * Fetches available delivery offers for the fulfiller.
 */
const getOffers = async (req, res) => {
  const userId = req.user.id;
  try {
    const fulfillerRes = await db.query("SELECT id, location FROM fulfillers WHERE user_id = $1", [userId]);
    if (fulfillerRes.rows.length === 0) return res.status(404).json({ error: 'Fulfiller profile not found' });

    const { id: fulfillerId, location } = fulfillerRes.rows[0];

    // Get nearby offers using refined dispatch logic
    const radiusInKm = 10; // Default
    // Note: We can implement mobility-based radius expansion here or in dispatchService
    const offers = await dispatchService.findNearbyFulfillersForOffer(fulfillerId, radiusInKm);

    res.status(200).json(offers);
  } catch (error) {
    console.error('Get Offers Error:', error);
    res.status(500).json({ error: 'Failed to fetch offers' });
  }
};

/**
 * Uploads a KYC document.
 */
const uploadKYC = async (req, res) => {
  const userId = req.user.id;
  const { document_type } = req.body;
  const file = req.file;

  if (!file) return res.status(400).json({ error: 'No file uploaded' });

  try {
    const fulfillerId = await ensureFulfillerExists(userId);
    const documentUrl = `/uploads/${file.filename}`;

    const { rows } = await db.query(
      `INSERT INTO kyc_documents (fulfiller_id, document_type, document_url, status)
       VALUES ($1, $2, $3, 'PENDING')
       RETURNING id, status`,
      [fulfillerId, document_type, documentUrl]
    );

    // Emit real-time alert for admin (v2.2.1)
    try {
        const socketService = require('../services/socketService');
        socketService.getIO().emit("new_kyc_alert", { fulfillerId, type: document_type });
    } catch (e) {}

    res.status(201).json({ message: 'Document uploaded successfully', document: rows[0] });
  } catch (error) {
    console.error('KYC Upload Error:', error);
    res.status(500).json({ error: 'Failed to upload KYC document' });
  }
};

module.exports = {
  startDiditVerification,
  handleDiditWebhook,
  updateStatus,
  updateProfile,
  uploadProfilePhoto,
  submitApplication,
  getOffers,
  getFulfillerOrders,
  getProfile,
  uploadKYC
};
