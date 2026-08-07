const admin = require('firebase-admin');
const db = require('../config/db');

// Note: Requires serviceAccountKey.json on the VPS
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

/**
 * Sends a push notification to a specific user.
 */
const sendNotification = async (userId, title, body, data = {}) => {
  try {
    const { rows } = await db.query("SELECT token FROM fcm_tokens WHERE user_id = $1", [userId]);
    if (rows.length === 0) return;

    const message = {
      notification: { title, body },
      data: data,
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
