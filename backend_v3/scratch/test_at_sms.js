const smsService = require('../src/services/smsService');
require('dotenv').config();

async function testAfricaTalkingSms() {
    console.log("=================================================");
    console.log("🧪 TESTING AFRICA'S TALKING SMS SERVICE (SENDER ID: Pikop)");
    console.log("=================================================\n");

    const testPhone = process.argv[2] || '+2348101373077';
    console.log(`[SMS Test] Sending test message to: ${testPhone}`);

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
        }

        process.exit(0);
    } catch (error) {
        console.error('\n❌ CRITICAL TEST EXCEPTION:', error.message, error.stack);
        process.exit(1);
    }
}

testAfricaTalkingSms();
