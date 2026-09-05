const { normalizePhone } = require('../src/utils/phone');

const testCases = [
    { input: '08123456789', expected: '+2348123456789' },
    { input: '+2348123456789', expected: '+2348123456789' },
    { input: '2348123456789', expected: '+2348123456789' },
    { input: '+234 812 345 6789', expected: '+2348123456789' },
    { input: '0812-345-6789', expected: '+2348123456789' },
    { input: '8123456789', expected: '+2348123456789' }
];

console.log('🧪 Testing Phone Normalization...');
let passed = 0;

testCases.forEach(tc => {
    const result = normalizePhone(tc.input);
    if (result === tc.expected) {
        console.log(`✅ PASS: ${tc.input} -> ${result}`);
        passed++;
    } else {
        console.error(`❌ FAIL: ${tc.input} -> Expected ${tc.expected}, got ${result}`);
    }
});

console.log(`\nResult: ${passed}/${testCases.length} tests passed.`);
process.exit(passed === testCases.length ? 0 : 1);
