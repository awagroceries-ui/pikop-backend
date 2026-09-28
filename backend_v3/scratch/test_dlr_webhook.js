const axios = require('axios');
const qs = require('querystring');

async function testDlrWebhook() {
    console.log("=================================================");
    console.log("🧪 TESTING AFRICA'S TALKING DLR WEBHOOK ENDPOINT");
    console.log("=================================================\n");

    const targets = [
        { name: "Local Server", url: "http://localhost:3000/api/v1/webhooks/africastalking/dlr" },
        { name: "Public Live Server", url: "https://api.pikop.com.ng/api/v1/webhooks/africastalking/dlr" }
    ];

    const sampleDlrPayload = {
        id: "ATXid_a1d9a7a4449376bdb92f5c03e5ddb7e5",
        status: "DeliveredToTerminal",
        phoneNumber: "+2348033001873",
        networkCode: "62120",
        failureReason: ""
    };

    for (const target of targets) {
        console.log(`📡 [Test] Sending POST to ${target.name} (${target.url})...`);
        try {
            const postData = qs.stringify(sampleDlrPayload);
            const response = await axios.post(target.url, postData, {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json, text/plain, */*'
                },
                timeout: 10000
            });

            console.log(`   ✅ STATUS: ${response.status} ${response.statusText}`);
            console.log(`   ✅ BODY  : ${JSON.stringify(response.data)}`);
            if (response.status === 200) {
                console.log(`   ✨ ${target.name} DLR Webhook is 100% REACHABLE & ACCEPTING POST REQUESTS!\n`);
            }
        } catch (error) {
            console.error(`   ❌ ${target.name} FAILED: ${error.response ? `HTTP ${error.response.status} - ${JSON.stringify(error.response.data)}` : error.message}\n`);
        }
    }

    process.exit(0);
}

testDlrWebhook();
