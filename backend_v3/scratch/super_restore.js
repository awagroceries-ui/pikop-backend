const fs = require('fs');
const path = require('path');
const db = require('../src/config/db');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const CONTROLLER_PATH = path.join(__dirname, '../src/controllers/fulfillerController.js');

const CONTROLLER_CONTENT = `const db = require('../config/db');
const diditService = require('../services/diditService');
const kycService = require('../services/kycService');
const axios = require('axios');

const startIdentityVerification = async (req, res) => {
  const userId = req.user.id;
  const { provider = process.env.PRIMARY_KYC_PROVIDER || 'prembly' } = req.body;
  const normalizedProvider = provider.toLowerCase();

  try {
    const userRes = await db.query("SELECT full_name, email FROM users WHERE id = \$1", [userId]);
    if (userRes.rows.length === 0) return res.status(404).json({ success: false, message: 'User not found' });
    const user = userRes.rows[0];

    if (normalizedProvider === 'didit') {
        const session = await diditService.createSession(userId);
        await db.query("UPDATE fulfillers SET didit_session_id = \$1, didit_verification_status = 'pending' WHERE user_id = \$2", [session.session_id, userId]);
        return res.status(200).json({ success: true, data: { url: session.url, token: session.session_token, session_id: session.session_id } });
    }

    if (normalizedProvider === 'prembly') {
        const nameParts = (user.full_name || 'Pikop User').split(' ');
        const firstName = nameParts[0];
        const lastName = nameParts.length > 1 ? nameParts.slice(1).join(' ') : 'User';

        const payload = {
            first_name: firstName,
            last_name: lastName,
            email: user.email,
            widget_id: process.env.PREMBLY_CONFIG_ID || '2183d331-33bd-4568-a67f-c21ffab5e274',
            widget_key: process.env.PREMBLY_WIDGET_ID || 'wdgt_02c17a8d92e54c659279db8cdf5839a2'
        };

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
            return res.status(200).json({
                success: true,
                data: { url: \`https://sdk-live.prembly.com/?session=\${sessionId}\`, session_id: sessionId }
            });
        }
        return res.status(400).json({ success: false, message: response.data.message });
    }

    res.status(200).json({ success: true, message: 'KYC fallback active', provider });
  } catch (error) { res.status(500).json({ success: false, message: error.message }); }
};

const updateFulfillerProfile = async (req, res) => {
    const userId = req.user.id;
    const { primary_class, mobility_type, vehicle_details, full_name, phone } = req.body;
    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        if (full_name || phone) {
            await client.query("UPDATE users SET full_name = COALESCE(\$1, full_name), phone = COALESCE(\$2, phone) WHERE id = \$3", [full_name, phone, userId]);
        }
        const fulfillerRes = await client.query(
            "UPDATE fulfillers SET primary_class = COALESCE(\$1, primary_class), mobility_type = COALESCE(\$2, mobility_type), registration_number = COALESCE(\$3, registration_number), make = COALESCE(\$4, make), model = COALESCE(\$5, model), color = COALESCE(\$6, color) WHERE user_id = \$7 RETURNING id",
            [primary_class, mobility_type, vehicle_details?.registration_number, vehicle_details?.make, vehicle_details?.model, vehicle_details?.color, userId]
        );
        if (fulfillerRes.rows.length === 0) {
            const userRes = await client.query("SELECT full_name, email, phone FROM users WHERE id = \$1", [userId]);
            const u = userRes.rows[0];
            await client.query("INSERT INTO fulfillers (user_id, full_name, email, phone, primary_class, password_hash) VALUES (\$1, \$2, \$3, \$4, \$5, 'external_auth')", [userId, u.full_name, u.email, u.phone, primary_class || 'rider']);
        }
        await client.query('COMMIT');
        res.status(200).json({ success: true, message: 'Profile updated' });
    } catch (error) { await client.query('ROLLBACK'); res.status(500).json({ success: false, message: error.message }); } finally { client.release(); }
};

const verifyVehiclePlate = async (req, res) => {
    const userId = req.user.id;
    const { plate_number } = req.body;
    try {
        const { rows: fulfiller } = await db.query("SELECT id, primary_class FROM fulfillers WHERE user_id = \$1", [userId]);
        if (fulfiller.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller not found' });
        const result = await kycService.executeCheck('verifyVehiclePlate', plate_number);
        await db.query("UPDATE fulfillers SET registration_number = \$1, kyc_status = \$2 WHERE id = \$3", [plate_number, result.status === 'SUCCESS' ? 'VERIFIED' : 'FAILED', fulfiller[0].id]);
        res.status(200).json(result);
    } catch (error) { res.status(500).json({ success: false, message: error.message }); }
};

const handleDiditWebhook = async (req, res) => {
  const rawBody = JSON.stringify(req.body);
  const signature = req.headers['x-signature-v2'];
  const timestamp = req.headers['x-timestamp'];
  if (!diditService.verifyWebhook(rawBody, signature, timestamp)) return res.status(401).send('Invalid signature');
  const { vendor_data, status } = req.body;
  const userId = vendor_data;
  const statusMap = { 'Approved': 'approved', 'Declined': 'declined' };
  const verifiedStatus = statusMap[status] || 'pending';
  try {
    await db.query("UPDATE fulfillers SET didit_verification_status = \$1, didit_verified_at = CURRENT_TIMESTAMP WHERE user_id = \$2", [verifiedStatus, userId]);
    res.status(200).send('OK');
  } catch (error) { res.status(500).send('Database error'); }
};

const uploadDocument = async (req, res) => {
  const userId = req.user.id;
  const { doc_type, expiry_date } = req.body;
  if (!req.file) return res.status(400).json({ success: false, message: 'File required' });
  const fileUrl = "/uploads/" + req.file.filename;
  try {
    const { rows: fulfiller } = await db.query("SELECT id FROM fulfillers WHERE user_id = \$1", [userId]);
    if (fulfiller.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller record missing' });
    await db.query("INSERT INTO kyc_documents (fulfiller_id, doc_type, file_url, status, expiry_date) VALUES (\$1, \$2, \$3, 'PENDING', \$4)", [fulfiller[0].id, doc_type, fileUrl, expiry_date]);
    res.status(201).json({ success: true, message: 'Document uploaded' });
  } catch (error) { res.status(500).json({ success: false, message: error.message }); }
};

const updateStatus = async (req, res) => {
  const { online_status, lat, lng } = req.body;
  const userId = req.user.id;
  try {
    const query = "UPDATE fulfillers SET online_status = \$1, current_location = ST_SetSRID(ST_MakePoint(\$2, \$3), 4326), last_ping_at = CURRENT_TIMESTAMP, last_active_at = CURRENT_TIMESTAMP WHERE user_id = \$4 RETURNING id, online_status";
    const { rows } = await db.query(query, [online_status || 'OFFLINE', lng || 0, lat || 0, userId]);
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller profile not found' });
    res.status(200).json({ success: true, data: { status: rows[0].online_status } });
  } catch (error) { res.status(500).send(error.message); }
};

const getProfile = async (req, res) => {
  const userId = req.user.id;
  try {
    const { rows } = await db.query("SELECT * FROM fulfillers WHERE user_id = \$1", [userId]);
    if (rows.length === 0) return res.status(404).json({ success: false, message: 'Profile not found' });
    res.status(200).json({ success: true, data: rows[0] });
  } catch (error) { res.status(500).send(error.message); }
};

const getFulfillerOrders = async (req, res) => {
    const userId = req.user.id;
    try {
        const { rows: f } = await db.query("SELECT id FROM fulfillers WHERE user_id = \$1", [userId]);
        if (f.length === 0) return res.status(404).json({ success: false, message: 'Fulfiller not found' });
        const { rows } = await db.query(
            "SELECT o.*, ST_Y(o.pickup_location::geometry) as pickup_lat, ST_X(o.pickup_location::geometry) as pickup_lng, ST_Y(o.delivery_location::geometry) as delivery_lat, ST_X(o.delivery_location::geometry) as delivery_lng FROM orders o WHERE o.fulfiller_id = \$1 OR o.queued_for_fulfiller_id = \$1 ORDER BY o.created_at DESC",
            [f[0].id]
        );
        res.status(200).json(rows);
    } catch (error) { res.status(500).send(error.message); }
};

const uploadProfilePhoto = async (req, res) => {
    const userId = req.user.id;
    if (!req.file) return res.status(400).json({ success: false, message: 'No file uploaded' });
    const photoUrl = "/uploads/" + req.file.filename;
    try {
        await db.query("UPDATE fulfillers SET profile_photo_url = \$1 WHERE user_id = \$2", [photoUrl, userId]);
        res.status(200).json({ success: true, url: photoUrl });
    } catch (error) { res.status(500).send(error.message); }
};

module.exports = {
  startIdentityVerification,
  updateFulfillerProfile,
  verifyVehiclePlate,
  handleDiditWebhook,
  uploadDocument,
  updateStatus,
  getProfile,
  getFulfillerOrders,
  uploadProfilePhoto
};
\`;

async function restore() {
    console.log('🚀 PIKOP VPS SUPER REPAIR INITIATED');

    try {
        // 1. Restore Fulfiller Controller
        console.log('[1/3] Restoring fulfillerController.js...');
        fs.writeFileSync(CONTROLLER_PATH, CONTROLLER_CONTENT);
        console.log('✅ Controller restored.');

        // 2. Reset Admin Password
        console.log('[2/3] Resetting Admin Credentials...');
        const hash = await bcrypt.hash('pikop123', 10);
        await db.query("DELETE FROM admin_users WHERE username = 'admin'");
        await db.query(
            "INSERT INTO admin_users (username, password_hash, role) VALUES (\$1, \$2, 'super_admin')",
            ['admin', hash]
        );
        console.log('✅ Admin reset to: admin / pikop123');

        // 3. Validate FCM
        console.log('[3/3] Checking Firebase Config...');
        const fcmRaw = process.env.FIREBASE_SERVICE_ACCOUNT;
        if (!fcmRaw) {
            console.error('❌ ERROR: FIREBASE_SERVICE_ACCOUNT missing in .env');
        } else {
            try {
                const parsed = JSON.parse(fcmRaw);
                const keys = Object.keys(parsed);
                console.log('✅ Firebase JSON Parsed successfully.');
                console.log('   Fields found:', keys.join(', '));
                if (!parsed.private_key) console.error('   ⚠️ WARNING: private_key is MISSING in JSON.');
            } catch (e) {
                console.error('❌ ERROR: FIREBASE_SERVICE_ACCOUNT is not valid JSON.');
            }
        }

        console.log('\n✨ REPAIR COMPLETE. Please run: pm2 restart pikop-v3');
        process.exit(0);
    } catch (e) {
        console.error('❌ REPAIR FAILED:', e.message);
        process.exit(1);
    }
}

restore();
