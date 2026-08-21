/**
 * Base Interface for Identity Verification Providers.
 * Ensures consistent response mapping across Dojah and Prembly.
 */
class IdentityVerificationProvider {
    constructor(name) {
        this.name = name;
    }

    async verifyNIN(vnin, userData) { throw new Error('Not implemented'); }
    async verifyDriversLicense(licenseNumber, userData) { throw new Error('Not implemented'); }
    async verifyVotersCard(vin, userData) { throw new Error('Not implemented'); }
    async verifyVehiclePlate(plateNumber) { throw new Error('Not implemented'); }
    async facialMatch(image1, image2) { throw new Error('Not implemented'); }
    async livenessCheck(videoOrImage) { throw new Error('Not implemented'); }
    async verifyWebhook(payload, signature) { throw new Error('Not implemented'); }

    /**
     * Standardized Response Model
     */
    mapResponse(success, data = null, error = null) {
        return {
            status: success ? 'SUCCESS' : 'FAILED',
            matchConfidence: data?.confidence || 0,
            rawProviderResponse: data,
            providerName: this.name,
            errorCode: error?.code || null,
            errorMessage: error?.message || null
        };
    }
}

module.exports = IdentityVerificationProvider;
