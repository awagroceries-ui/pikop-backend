const db = require('../config/db');
const geminiService = require('../services/geminiService');

/**
 * Generates a dynamic, distance-based quote.
 */
const getQuote = async (req, res) => {
  const { pickup_address, delivery_address, item_description, pickup_lat, pickup_lng, delivery_lat, delivery_lng } = req.body;
  const userId = req.user?.id;

  // 1. Calculate Distance using PostGIS Geography
  let distanceKm = 0;
  try {
    const distRes = await db.query(
      "SELECT ST_Distance(ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography, ST_SetSRID(ST_MakePoint($3, $4), 4326)::geography) / 1000 as dist",
      [pickup_lng, pickup_lat, delivery_lng, delivery_lat]
    );
    distanceKm = parseFloat(distRes.rows[0].dist || 0);
  } catch (e) {
    console.error('[Quote] Distance error:', e.message);
  }

  // 2. Classify Size
  const aiResult = await geminiService.classifyItemSize(item_description);

  // 3. Apply Pricing Formula
  const baseFees = { 'SMALL': 500, 'MEDIUM': 1000, 'LARGE': 1500 };
  const perKmRate = 150;
  const totalFare = Math.ceil((baseFees[aiResult.size_tier] || 1000) + (distanceKm * perKmRate));

  // 4. Save Quote
  const quoteRes = await db.query(
    `INSERT INTO quotes (user_id, pickup_address, delivery_address, pickup_location, delivery_location, item_description, size_tier, total_fare)
     VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), ST_SetSRID(ST_MakePoint($6, $7), 4326), $8, $9, $10)
     RETURNING id, expires_at`,
    [userId, pickup_address, delivery_address, pickup_lng, pickup_lat, delivery_lng, delivery_lat, item_description, aiResult.size_tier, totalFare]
  );

  res.status(200).json({
    success: true,
    data: {
      quote_id: quoteRes.rows[0].id,
      size_tier: aiResult.size_tier,
      distance_km: distanceKm.toFixed(2),
      total_fare: totalFare,
      expires_at: quoteRes.rows[0].expires_at
    }
  });
};

module.exports = {
  getQuote
};
