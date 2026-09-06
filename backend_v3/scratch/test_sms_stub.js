const smsService = require('../src/services/smsService');

async function testSms() {
    console.log('🧪 Testing SMS Gateway Infrastructure (Stub Mode)...');

    try {
        const result = await smsService.sendSecurePaySms('+2348123456789', '1500', 'ORD-12345');

        if (result.success && result.messageId.startsWith('stub_')) {
            console.log(`✅ SMS PASS: Stub correctly generated message and ID.`);
            console.log(`[Result] ${JSON.stringify(result)}`);
        } else {
            console.error(`❌ SMS FAIL: Unexpected result format.`);
            console.log(result);
        }

        process.exit(0);
    } catch (error) {
        console.error('❌ TEST ERROR:', error.message);
        process.exit(1);
    }
}

testSms();
