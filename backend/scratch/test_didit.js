const { createSession } = require('../src/services/diditService');
require('dotenv').config({ path: '../.env' });

async function testDidit() {
    console.log('--- Didit Diagnostic Tool ---');
    console.log('Checking configuration...');

    if (!process.env.DIDIT_API_KEY) {
        console.error('❌ ERROR: DIDIT_API_KEY is missing in .env');
        process.exit(1);
    }

    console.log('Attempting to create a test verification session...');

    try {
        // Use a dummy user ID for testing
        const session = await createSession(9999);

        if (session && session.session_id) {
            console.log('✅ SUCCESS: Didit API handshake complete.');
            console.log('Session ID:', session.session_id);
            console.log('Session Token:', session.session_token ? 'Received' : 'MISSING');
            console.log('Verification URL:', session.url);
            console.log('👉 Tip: Open the URL above in a browser to see if the KYC UI loads correctly.');
        } else {
            console.error('❌ FAILURE: Didit returned an unexpected response.');
            console.log('Response:', session);
        }
    } catch (error) {
        console.error('❌ CRITICAL FAILURE: Didit integration failed.');
        console.error('Reason:', error.message);

        if (error.message.includes('401')) {
            console.error('👉 TIP: Unauthorized. Your DIDIT_API_KEY is likely invalid.');
        } else if (error.message.includes('404')) {
            console.error('👉 TIP: Workflow not found. Check if your WORKFLOW_ID in diditService.js is correct.');
        }
    }
}

testDidit();
