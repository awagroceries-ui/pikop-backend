const db = require('../config/db');
const dispatchService = require('../services/dispatchService');

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
    // In production, we'd use the fulfiller's actual last location
    const query = `
      SELECT o.id, o.pickup_address, o.delivery_address, o.total_fare, o.created_at
      FROM orders o, fulfillers f
      WHERE f.id = $1
      AND o.status = 'SEARCHING'
      AND ST_DWithin(f.location, o.pickup_location, 10000)
      ORDER BY o.created_at DESC
    `;
    const { rows } = await db.query(query, [fulfillerId]);

    const offers = rows.map(r => ({
      id: r.id.toString(),
      pickup_address: r.pickup_address,
      delivery_address: r.delivery_address,
      total_fare: parseFloat(r.total_fare),
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
  // ... existing code
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
  updateStatus,
  getOffers,
  getFulfillerOrders,
  uploadKYC
};
