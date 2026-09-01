const axios = require('axios');
const IdentityVerificationProvider = require('./IdentityVerificationProvider');

class PremblyProvider extends IdentityVerificationProvider {
    constructor() {
        super('Prembly');
        this.apiKey = process.env.PREMBLY_SECRET_KEY;
        this.baseUrl = 'https://api.prembly.com'; // Adjust to real API base
    }

    async verifyNIN(vnin, userData) {
        try {
            const response = await this._post('/identitypass/verification/nin', {
                number: vnin,
                first_name: userData.firstName,
                last_name: userData.lastName
            });
            return this.mapResponse(response.status === 'success', response.data);
        } catch (e) {
            return this.mapResponse(false, null, e);
        }
    }

    async verifyVehiclePlate(plateNumber) {
        try {
            // Prembly vehicle verification endpoint
            const response = await this._post('/identitypass/verification/vehicle_plate', {
                registration_number: plateNumber
            });
            return this.mapResponse(response.status === 'success', response.data);
        } catch (e) {
            return this.mapResponse(false, null, e);
        }
    }

    async facialMatch(image1, image2) {
        try {
            const response = await this._post('/identitypass/face/comparison', {
                image_one: image1,
                image_two: image2
            });
            return this.mapResponse(response.status === 'success', response.data);
        } catch (e) {
            return this.mapResponse(false, null, e);
        }
    }

    async verifyDriversLicense(licenseNumber, userData) {
        try {
            const response = await this._post('/identitypass/verification/drivers_license', {
                number: licenseNumber,
                first_name: userData.firstName,
                last_name: userData.lastName
            });
            return this.mapResponse(response.status === 'success', response.data);
        } catch (e) { return this.mapResponse(false, null, e); }
    }

    async verifyVotersCard(vin, userData) {
        try {
            const response = await this._post('/identitypass/verification/voters_card', {
                number: vin,
                first_name: userData.firstName,
                last_name: userData.lastName
            });
            return this.mapResponse(response.status === 'success', response.data);
        } catch (e) { return this.mapResponse(false, null, e); }
    }

    /**
     * Verifies Prembly Webhook authenticity using HMAC-SHA512.
     */
    verifyWebhook(rawBody, signature) {
        if (!rawBody || !signature) return false;
        const crypto = require('crypto');
        const expected = crypto.createHmac('sha512', this.apiKey).update(rawBody).digest('hex');

        try {
            return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
        } catch (e) {
            return false;
        }
    }

    async _post(endpoint, data) {
        const res = await axios.post(`${this.baseUrl}${endpoint}`, data, {
            headers: {
                'x-api-key': this.apiKey
            }
        });
        return res.data;
    }
}

module.exports = new PremblyProvider();
