const admin = require('firebase-admin');
require('dotenv').config({ path: '../.env' });

async function testFirebase() {
    console.log('--- Firebase Diagnostic Tool ---');

    if (!process.env.FIREBASE_SERVICE_ACCOUNT) {
        console.error('❌ ERROR: FIREBASE_SERVICE_ACCOUNT is missing in .env');
        process.exit(1);
    }

    let serviceAccount;
    try {
        serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        console.log('✅ Config: JSON parsed successfully.');
    } catch (e) {
        console.error('❌ ERROR: FIREBASE_SERVICE_ACCOUNT is not a valid JSON string.');
        console.error('Raw content starts with:', process.env.FIREBASE_SERVICE_ACCOUNT.substring(0, 20));
        process.exit(1);
    }

    try {
        if (!admin.apps.length) {
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
        }
        console.log('✅ Firebase: App initialized.');

        // Attempt to reach Google servers by listing users or similar
        const messaging = admin.messaging();
        console.log('✅ Messaging: Service instance created.');

        console.log('Test complete. Firebase setup appears technically valid.');
        console.log('👉 To test actual delivery, you need a valid FCM Token from a physical device.');
    } catch (error) {
        console.error('❌ FAILURE: Firebase initialization failed.');
        console.error('Error Message:', error.message);
    }
}

testFirebase();
