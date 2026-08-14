const admin = require('firebase-admin');
const db = require('../config/db');

// Safe Initialization
try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        let serviceAccount;
        try {
            serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        } catch (e) {
            // Check if it's a file path
            const fs = require('fs');
            if (fs.existsSync(process.env.FIREBASE_SERVICE_ACCOUNT)) {
                serviceAccount = JSON.parse(fs.readFileSync(process.env.FIREBASE_SERVICE_ACCOUNT, 'utf8'));
            } else {
                throw new Error('FIREBASE_SERVICE_ACCOUNT is neither valid JSON nor a valid file path.');
            }
        }

        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            console.log('✅ Firebase Admin initialized successfully.');
        }
    } else {
        console.warn('⚠️ FIREBASE_SERVICE_ACCOUNT missing. Push notifications disabled.');
    }
} catch (error) {
    console.error('❌ Firebase Init Error:', error.message);
    console.warn('⚠️ App will continue without Push Notification support.');
}

/**
 * Sends a push notification to a specific user.
 */
const sendNotification = async (userId, title, body, data = {}) => {
  if (!admin.apps.length) return;
  try {
    const { rows } = await db.query("SELECT token FROM fcm_tokens WHERE user_id = $1", [userId]);
    if (rows.length === 0) return;

    const message = {
      notification: {
        title: title,
        body: body
      },
      data: {
        title: title,
        body: body,
        ...data
      },
      token: rows[0].token,
    };

    await admin.messaging().send(message);
    console.log(`Notification sent to user ${userId}`);
  } catch (error) {
    console.error('FCM Error:', error.message);
  }
};

/**
 * Saves or updates an FCM token for a user.
 */
const saveToken = async (userId, token) => {
  await db.query(
    "INSERT INTO fcm_tokens (user_id, token) VALUES ($1, $2) ON CONFLICT (user_id) DO UPDATE SET token = $2, updated_at = CURRENT_TIMESTAMP",
    [userId, token]
  );
};

module.exports = {
  sendNotification,
  saveToken
};
