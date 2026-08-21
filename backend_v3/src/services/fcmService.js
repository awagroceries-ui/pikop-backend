const admin = require('firebase-admin');
const db = require('../config/db');

try {
    let rawConfig = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (rawConfig) {
        // Handle potential wrapping quotes or extra spaces from shell/env
        rawConfig = rawConfig.trim();
        if (rawConfig.startsWith("'") && rawConfig.endsWith("'")) rawConfig = rawConfig.slice(1, -1);
        if (rawConfig.startsWith("\"") && rawConfig.endsWith("\"")) rawConfig = rawConfig.slice(1, -1);

        let serviceAccount;
        try {
            serviceAccount = JSON.parse(rawConfig);
        } catch (e) {
            console.error('❌ FCM: FIREBASE_SERVICE_ACCOUNT is not valid JSON. Error:', e.message);
            throw e;
        }

        if (serviceAccount.private_key) {
            // FIX 1: PEM formatting (\\n vs \n)
            serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, '\n');
            // FIX 2: Repair mangled '+' (Some VPS environments)
            if (serviceAccount.private_key.includes(' ') && !serviceAccount.private_key.includes('+')) {
                serviceAccount.private_key = serviceAccount.private_key.replace(/ /g, '+');
            }
            console.log(`[FCM] Service Account parsed. Keys: ${Object.keys(serviceAccount).join(', ')}`);
        }

        if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
            console.error('❌ FCM: Incomplete JSON. Keys found:', Object.keys(serviceAccount));
            throw new Error('Incomplete Firebase JSON');
        }

        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            console.log('✅ FCM Service: Firebase Admin v1 initialized.');
        }
    } else {
        console.warn('⚠️ FCM: FIREBASE_SERVICE_ACCOUNT missing in .env.');
    }
} catch (error) {
    console.error('❌ FCM Init Failed:', error.message);
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
