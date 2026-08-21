const prembly = require('./kyc/PremblyProvider');
const dojah = require('./kyc/DojahProvider');

const PRIMARY_PROVIDER = process.env.PRIMARY_KYC_PROVIDER || 'prembly';

/**
 * Executes a KYC check with automatic fallback.
 */
const executeCheck = async (methodName, ...args) => {
    const primary = PRIMARY_PROVIDER === 'prembly' ? prembly : dojah;
    const secondary = PRIMARY_PROVIDER === 'prembly' ? dojah : prembly;

    console.log(`[KYC] Initiating ${methodName} via ${primary.name}`);

    try {
        const result = await primary[methodName](...args);

        if (result.status === 'SUCCESS') return result;

        // Fallback Trigger: API error or timeout, NOT legitimate verification failure
        if (result.errorCode && result.errorCode !== 'VERIFICATION_FAILED') {
            console.warn(`[KYC] ${primary.name} failed (${result.errorCode}). Triggering kyc_provider_fallback_triggered to ${secondary.name}`);
            return await secondary[methodName](...args);
        }

        return result;
    } catch (e) {
        console.error(`[KYC] Fatal error in primary provider: ${e.message}. Falling back.`);
        return await secondary[methodName](...args);
    }
};

module.exports = {
    executeCheck
};
