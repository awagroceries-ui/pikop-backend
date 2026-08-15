const db = require('../config/db');
const diditService = require('../services/diditService');

/**
 * Initializes a Didit KYC session.
 */
const startIdentityVerification = async (req, res) => {
  const userId = req.user.id;

  try {
    const session = await diditService.createSession(userId);

    await db.query(
      "UPDATE fulfillers SET didit_session_id = $1, didit_verification_status = 'pending' WHERE user_id = $2",
      [session.session_id, userId]
    );

    res.status(200).json({
      success: true,
      data: {
        url: session.url,
        token: session.session_token,
        session_id: session.session_id
      }
    });
  } catch (error) {
    res.status(502).json({ success: false, message: error.message });
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

    res.status(201).json({ success: true, message: 'Document uploaded' });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * Updates fulfiller online status and current GPS location.
 */
const updateStatus = async (req, res) => {
  const { online_status, lat, lng } = req.body;
  const userId = req.user.id;

  try {
    const query = `
      UPDATE fulfillers
      SET online_status = $1,
          current_location = ST_SetSRID(ST_MakePoint($2, $3), 4326),
          last_ping_at = CURRENT_TIMESTAMP,
          last_active_at = CURRENT_TIMESTAMP
      WHERE user_id = $4
      RETURNING id, online_status
    `;

    const { rows } = await db.query(query, [online_status || 'OFFLINE', lng || 0, lat || 0, userId]);

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

module.exports = {
  startIdentityVerification,
  handleDiditWebhook,
  uploadDocument,
  updateStatus,
  getProfile
};
