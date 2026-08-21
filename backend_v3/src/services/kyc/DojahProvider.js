const axios = require('axios');
const IdentityVerificationProvider = require('./IdentityVerificationProvider');

class DojahProvider extends IdentityVerificationProvider {
    constructor() {
        super('Dojah');
        this.apiKey = process.env.DIDIT_API_KEY; // Reusing existing var if it holds Dojah key
        this.appId = process.env.FIREBASE_SERVICE_ACCOUNT ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT).project_id : '';
    }

    async verifyNIN(vnin, userData) {
        try {
            const res = await axios.get(`https://api.dojah.io/api/v1/kyc/nin/vnin`, {
                params: { vnin },
                headers: { Authorization: this.apiKey, 'App-Id': this.appId }
            });
            return this.mapResponse(true, res.data.entity);
        } catch (e) {
            return this.mapResponse(false, null, e);
        }
    }

    // Dojah might not support plate verification in the same way or at all
    async verifyVehiclePlate(plateNumber) {
        return this.mapResponse(false, null, { code: 'NOT_SUPPORTED', message: 'Dojah does not support plate verification' });
    }

    async mapResponse(success, data = null, error = null) {
        return super.mapResponse(success, data, error);
    }
}

module.exports = new DojahProvider();
