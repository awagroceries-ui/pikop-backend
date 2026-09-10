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
 * Sends a high-priority push notification (Alert + Data).
 */
const sendNotification = async (userId, title, body, data = {}) => {
  if (!admin.apps.length) return;

  try {
    const { rows } = await db.query("SELECT token FROM user_fcm_tokens WHERE user_id = $1", [userId]);
    if (rows.length === 0) return;

    const message = {
      token: rows[0].token,
      notification: {
        title: title,
        body: body
      },
      data: {
        ...data,
        channel_id: "pikop_v3_logistics"
      },
      android: {
        priority: "high",
        notification: {
            sound: "default",
            click_action: "FLUTTER_NOTIFICATION_CLICK" // Legacy support
        }
      },
      apns: {
        payload: {
          aps: {
            contentAvailable: true,
            sound: "default"
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

/**
 * Sends reminder for Secure Pay confirmation.
 */
const sendSecurePayReminder = async (userId, orderId) => {
    return sendNotification(
        userId,
        "Confirm your Delivery",
        "Your item has been delivered! Tap to confirm receipt and release payment.",
        { type: "ORDER_UPDATE", order_id: orderId.toString() }
    );
};

/**
 * Sends alert for successful payout.
 */
const sendPayoutAlert = async (userId, amount) => {
    return sendNotification(
        userId,
        "Payout Successful",
        `Your withdrawal of ₦${parseFloat(amount).toLocaleString()} has been processed and sent to your bank.`,
        { type: "WALLET_UPDATE" }
    );
};

module.exports = {
  sendNotification,
  sendSecurePayReminder,
  sendPayoutAlert
};
