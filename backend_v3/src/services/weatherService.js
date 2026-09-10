const axios = require('axios');
const db = require('../config/db');

const WEATHER_API_KEY = process.env.GOOGLE_MAPS_API_KEY; // Reusing GMap key if compatible or needs specific weather API
const CITIES = [
    { name: 'Lagos', lat: 6.5244, lng: 3.3792 },
    { name: 'Abuja', lat: 9.0765, lng: 7.3986 },
    { name: 'Port Harcourt', lat: 4.8156, lng: 7.0498 }
];

/**
 * Polls Google Weather API and updates zone multipliers.
 */
const pollWeather = async () => {
    console.log('[WeatherJob] Starting periodic poll...');

    for (const city of CITIES) {
        try {
            // NOTE: In a real implementation, we would call the Google Weather API here.
            // For Phase 4 Static V1, we simulate a check or use a placeholder.
            // Assuming simulated "Clear" status for now.

            const multiplier = 1.0; // Dynamic logic goes here
            const alert = 'clear';

            // Update all flood_prone zones in this city
            // We use ST_DWithin to find zones within 50km of city centroid.
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
