const admin = require('firebase-admin');
const db = require('../config/db');

// Safe Initialization (Milestone 9)
try {
    const rawConfig = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (rawConfig) {
        const serviceAccount = JSON.parse(rawConfig);
        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            console.log('✅ FCM Service: Firebase Admin v1 initialized.');
        }
    }
} catch (error) {
    console.error('❌ FCM Init Error:', error.message);
}

/**
 * Sends a high-priority push notification (Data-only for consistency).
 */
const sendNotification = async (userId, title, body, data = {}) => {
  if (!admin.apps.length) return;

  try {
    const { rows } = await db.query("SELECT token FROM user_fcm_tokens WHERE user_id = $1", [userId]);
    if (rows.length === 0) return;

    // Milestone 9: Use "data" payloads for manual construction on client
    const message = {
      token: rows[0].token,
      data: {
        title,
        body,
        channel_id: "pikop_v3_logistics",
        ...data
      },
      android: {
        priority: "high"
      },
      apns: {
        payload: {
          aps: {
            contentAvailable: true,
          }
        }
      }
    };

    const response = await admin.messaging().send(message);
    console.log(`[FCM] Delivered to User ${userId}: ${response}`);
  } catch (error) {
    console.error(`[FCM] Delivery Failed for User ${userId}:`, error.message);
  }
};

module.exports = {
  sendNotification
};
