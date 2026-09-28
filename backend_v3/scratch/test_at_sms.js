const smsService = require('../src/services/smsService');
require('dotenv').config();

async function testAfricaTalkingSms() {
    console.log("=================================================");
    console.log("🧪 TESTING AFRICA'S TALKING SMS SERVICE (SENDER ID: Pikop)");
    console.log("=================================================\n");

    const username = (process.env.AT_USERNAME || process.env.AFRICASTALKING_USERNAME || 'pikop').trim();
    const apiKey = (process.env.AT_API_KEY || process.env.AFRICASTALKING_API_KEY || '').trim();
    const senderId = (process.env.AT_SENDER_ID || 'Pikop').trim();

    console.log(`[Diagnostic] AT_USERNAME  : "${username}"`);
    console.log(`[Diagnostic] AT_SENDER_ID : "${senderId}"`);
    console.log(`[Diagnostic] AT_API_KEY   : ${apiKey ? `Present (${apiKey.length} chars, Prefix: ${apiKey.substring(0, 6)}...)` : '❌ MISSING IN .env'}`);

    const testPhone = process.argv[2] || '+2348101373077';
    console.log(`\n[SMS Test] Sending test message to: ${testPhone}`);

    try {
        const result = await smsService.sendSms(
            testPhone,
            "Pikop: Test verification message via Africa's Talking API. Sender ID: Pikop.",
            "test_message"
        );

        console.log('\n[SMS Test Result]:', JSON.stringify(result, null, 2));

        if (result.success) {
            console.log("\n✅ AFRICA'S TALKING SMS TEST SUCCESSFUL!");
        } else {
            console.error("\n❌ AFRICA'S TALKING SMS TEST FAILED:", result.error);
            if (result.error && result.error.includes("supplied authentication is invalid")) {
                console.log("\n👉 HOW TO FIX THIS:");
                console.log("1. Log into your Africa's Talking Dashboard at https://africastalking.com");
                console.log("2. Copy your App Username and API Key (from Settings -> API Key).");
                console.log("3. Add them to /var/www/pikop-api/backend_v3/backend_v3/.env:");
                console.log("   AT_USERNAME=your_username");
                console.log("   AT_API_KEY=your_api_key");
                console.log("   AT_SENDER_ID=Pikop");
            }
        }

        process.exit(0);
    } catch (error) {
        console.error('\n❌ CRITICAL TEST EXCEPTION:', error.message, error.stack);
        process.exit(1);
    }
}

testAfricaTalkingSms();
