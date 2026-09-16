const axios = require('axios');
const db = require('../config/db');

/**
 * Polls Google Weather API and updates zone multipliers.
 */
const pollWeather = async () => {
    console.log('[WeatherJob] Starting periodic poll...');

    try {
        const { rows: cities } = await db.query("SELECT name, lat, lng FROM operating_cities WHERE is_active = true");

        for (const city of cities) {
            try {
                // NOTE: In a real implementation, we would call the Google Weather API here.
                const multiplier = 1.0;
                const alert = 'clear';

                await db.query(`
                    UPDATE zones
                    SET weather_multiplier = $1
                    WHERE flood_prone = true
                      AND weather_mode = 'auto'
                      AND ST_DWithin(boundary::geography, ST_SetSRID(ST_MakePoint($2, $3), 4326)::geography, 50000)
                `, [multiplier, city.lng, city.lat]);

                console.log(`[WeatherJob] City ${city.name} updated. Alert: ${alert}. Multiplier: ${multiplier}`);

            } catch (e) {
                console.error(`[WeatherJob] Failed for ${city.name}:`, e.message);
            }
        }
    } catch (err) {
        console.error('[WeatherJob] Failed to fetch active cities:', err.message);
    }
};

/**
 * Initializes the background job.
 */
const startWeatherJob = (intervalMinutes = 15) => {
    setInterval(pollWeather, intervalMinutes * 60 * 1000);
    pollWeather(); // Initial run
};

module.exports = {
    startWeatherJob
};
