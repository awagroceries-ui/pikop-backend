const db = require('../config/db');
const dispatchService = require('../services/dispatchService');
const diditService = require('../services/diditService');

/**
 * Initializes a Didit KYC session for the fulfiller.
 */
const startDiditVerification = async (req, res) => {
  const userId = req.user.id;

  try {
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
    res.status(500).json({ error: error.message });
  }
};

/**
 * Handles signed decision webhooks from Didit.
 */
const handleDiditWebhook = async (req, res) => {
  const raw = JSON.stringify(req.body); // For alpha, if rawBody not middleware-attached
  const sig = req.headers['x-signature-v2'];
  const ts = req.headers['x-timestamp'];

  if (!diditService.verifyWebhookSignature(raw, sig, ts)) {
    return res.status(401).send('Invalid signature');
  }

  const event = req.body;
  const userId = event.vendor_data;
  const statusMap = {
    'Approved': 'approved',
    'Declined': 'declined',
    'In Review': 'needs_review'
  };

  const newStatus = statusMap[event.status] || 'pending';

  try {
    await db.query(
      "UPDATE fulfillers SET didit_verification_status = $1, didit_verified_at = CURRENT_TIMESTAMP WHERE user_id = $2",
      [newStatus, userId]
    );

    if (newStatus === 'approved') {
      const { rows } = await db.query(`
        SELECT COUNT(*) FROM kyc_documents
        WHERE fulfiller_id = (SELECT id FROM fulfillers WHERE user_id = $1)
        AND status != 'APPROVED'`, [userId]);

      if (rows[0].count === "0") {
        await db.query("UPDATE fulfillers SET kyc_status = 'VERIFIED' WHERE id = $1", [userId]);
      }
    }

    res.status(200).send('OK');
  } catch (error) {
    console.error('Didit Webhook Update Error:', error);
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
    // 1. Get fulfiller profile linked to this user
    const fulfillerRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    if (fulfillerRes.rows.length === 0) return res.status(404).json({ error: 'Fulfiller profile not found' });

    const fulfillerId = fulfillerRes.rows[0].id;

    // 2. Update status and location if provided
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
    console.error('Update Status Error:', error);
    res.status(500).json({ error: 'Failed to update status' });
  }
};

/**
 * Fetches available delivery offers for the fulfiller.
 */
const getOffers = async (req, res) => {
  const userId = req.user.id;
  try {
    const fulfillerRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    if (fulfillerRes.rows.length === 0) return res.status(404).json({ error: 'Fulfiller profile not found' });

    const fulfillerId = fulfillerRes.rows[0].id;

    // For alpha: Find all SEARCHING orders within a fixed 10km radius
    const query = `
      SELECT o.id, o.pickup_address, o.delivery_address, o.total_fare, o.created_at, o.item_photo_url, o.pickup_display_summary
      FROM orders o, fulfillers f
      WHERE f.id = $1
      AND o.status = 'SEARCHING'
      AND ST_DWithin(f.location, o.pickup_location, 10000)
      ORDER BY o.created_at DESC
    `;
    const { rows } = await db.query(query, [fulfillerId]);

    const offers = rows.map(r => ({
      id: r.id.toString(),
      pickup_address: r.pickup_display_summary || 'Restricted Area', // Masked for offer
      delivery_address: 'Hidden until accepted', // Masked for offer
      total_fare: parseFloat(r.total_fare),
      item_photo_url: r.item_photo_url,
      expires_at: new Date(new Date(r.created_at).getTime() + 5 * 60000).toISOString() // 5m offer window
    }));

    res.status(200).json(offers);
  } catch (error) {
    console.error('Get Offers Error:', error);
    res.status(500).json({ error: 'Failed to fetch offers' });
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
 * Returns the fulfiller profile status.
 */
const getProfile = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query(
      "SELECT id, online_status, kyc_status, didit_verification_status FROM fulfillers WHERE user_id = $1",
      [userId]
    );
    if (rows.length === 0) return res.status(404).json({ error: 'Fulfiller profile not found' });
    res.status(200).json(rows[0]);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch profile' });
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
    const fulfillerRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    if (fulfillerRes.rows.length === 0) return res.status(404).json({ error: 'Fulfiller profile not found' });

    const fulfillerId = fulfillerRes.rows[0].id;
    const documentUrl = `/uploads/${file.filename}`;

    const { rows } = await db.query(
      `INSERT INTO kyc_documents (fulfiller_id, document_type, document_url, status)
       VALUES ($1, $2, $3, 'PENDING')
       RETURNING id, status`,
      [fulfillerId, document_type, documentUrl]
    );

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
  getOffers,
  getFulfillerOrders,
  getProfile,
  uploadKYC
};
