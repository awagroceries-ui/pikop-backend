const admin = require('firebase-admin');
const db = require('../config/db');

// Safe Initialization
try {
    const rawConfig = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (rawConfig) {
        let serviceAccount;
        try {
            // 1. Try direct JSON parse
            serviceAccount = JSON.parse(rawConfig);
        } catch (e) {
            // 2. Try file path
            const fs = require('fs');
            if (fs.existsSync(rawConfig)) {
                serviceAccount = JSON.parse(fs.readFileSync(rawConfig, 'utf8'));
            } else {
                console.error('❌ FIREBASE_SERVICE_ACCOUNT Error: Not valid JSON and file not found.');
                throw e;
            }
        }

        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            console.log('✅ Firebase Admin initialized successfully.');
        }
    } else {
        console.warn('⚠️ FIREBASE_SERVICE_ACCOUNT missing in .env. Push notifications disabled.');
    }
} catch (error) {
    console.error('❌ Firebase Init Failed:', error.message);
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        console.error('👉 Tip: Ensure FIREBASE_SERVICE_ACCOUNT is a single-line JSON string wrapped in single quotes in .env');
    }
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
      android: {
        priority: "high",
        notification: {
            channel_id: "pikop_notifications",
            priority: "high",
            visibility: "public"
        }
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
