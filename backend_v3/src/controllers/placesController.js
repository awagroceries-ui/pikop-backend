const axios = require('axios');

const GOOGLE_API_KEY = process.env.GOOGLE_PLACES_API_KEY || process.env.GOOGLE_MAPS_API_KEY;

/**
 * Proxies Google Places Autocomplete requests with Nigeria bias.
 */
const autocomplete = async (req, res) => {
    const { query, sessionToken, lat, lng } = req.query;

    if (!query) return res.status(400).json({ success: false, message: 'Query is required' });

    // One-time log for diagnostic verification
    const keySuffix = (GOOGLE_API_KEY || '').slice(-4);
    console.log(`[Places] Diagnostic: Autocomplete request. Key suffix: ...${keySuffix}`);

    try {
        const params = {
            input: query,
            key: GOOGLE_API_KEY,
            sessiontoken: sessionToken,
            components: 'country:ng',
            language: 'en'
        };

        // Bias to user location if provided
        if (lat && lng) {
            params.location = `${lat},${lng}`;
            params.radius = 50000; // 50km radius
        }

        const response = await axios.get('https://maps.googleapis.com/maps/api/place/autocomplete/json', { params });

        if (response.data.status !== 'OK' && response.data.status !== 'ZERO_RESULTS') {
            console.error('[Places] Google API Full Response:', JSON.stringify(response.data, null, 2));
            console.error('[Places] Google API Error:', response.data.status, 'Message:', response.data.error_message || 'No detail provided');
            return res.status(200).json({
                success: false,
                predictions: [],
                error: `Google API Error: ${response.data.status}. Check VPS logs for full JSON.`
            });
        }

        const predictions = response.data.predictions.map(p => ({
            place_id: p.place_id,
            description: p.description,
            main_text: p.structured_formatting.main_text,
            secondary_text: p.structured_formatting.secondary_text
        }));

        res.status(200).json({ success: true, predictions });
    } catch (error) {
        console.error('[Places] Autocomplete error:', error.message);
        res.status(500).json({ success: false, message: 'Places service unavailable' });
    }
};

/**
 * Proxies Google Place Details requests to resolve coordinates and components.
 */
const details = async (req, res) => {
    const { placeId, sessionToken } = req.query;

    if (!placeId) return res.status(400).json({ success: false, message: 'Place ID is required' });

    try {
        const params = {
            place_id: placeId,
            key: GOOGLE_API_KEY,
            sessiontoken: sessionToken,
            fields: 'formatted_address,geometry,address_components,name'
        };

        const response = await axios.get('https://maps.googleapis.com/maps/api/place/details/json', { params });
        const result = response.data.result;

        if (!result) return res.status(404).json({ success: false, message: 'Place not found' });

        const structured = {
            formatted_address: result.formatted_address,
            lat: result.geometry.location.lat,
            lng: result.geometry.location.lng,
            name: result.name,
            address_components: {}
        };

        // Map components (Street, City, State)
        result.address_components.forEach(c => {
            if (c.types.includes('route')) structured.address_components.street = c.long_name;
            if (c.types.includes('locality')) structured.address_components.city = c.long_name;
            if (c.types.includes('administrative_area_level_1')) structured.address_components.state = c.long_name;
        });

        res.status(200).json({ success: true, ...structured });
    } catch (error) {
        console.error('[Places] Details error:', error.message);
        res.status(500).json({ success: false, message: 'Details service unavailable' });
    }
};

module.exports = {
    autocomplete,
    details
};
