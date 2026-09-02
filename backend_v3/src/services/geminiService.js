const { GoogleGenerativeAI } = require("@google/generative-ai");

const API_KEY = process.env.GEMINI_API_KEY;
// For stability in Node.js, we explicitly set the API version at the model level
const genAI = new GoogleGenerativeAI(API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" }, { apiVersion: "v1" });

/**
 * Classifies an item into a size tier using v3 stable AI.
 * Enhanced for weight and bulky item detection.
 */
const classifyItemSize = async (description) => {
  // 0. KEYWORD FALLBACK (Pikop Priority Shield)
  // Ensures common Nigerian bulky items are caught even if AI fails or throttles.
  const desc = (description || '').toLowerCase();
  if (desc.includes('generator') || desc.includes('engine') || desc.includes('fridge') || desc.includes('freezer') || desc.includes('table') || desc.includes('chair') || desc.includes('bulk') || desc.includes('sack')) {
      console.log('[Gemini] Fallback Triggered: LARGE item detected via keywords.');
      return { size_tier: 'LARGE', confidence: 1.0 };
  }

  if (!API_KEY) return { size_tier: 'MEDIUM', confidence: 0.5 };

  const prompt = `
    Context: You are the logistics classifier for Pikop (a Nigerian delivery app).
    Analyze this item description: "${description}"

    Rules for size_tier (Pikop Priority):
    1. SMALL: Envelopes, keys, single small document, food packs, or items < 2kg.
    2. MEDIUM: Standard boxes, grocery bags, microwaves, medium luggage, or items 2kg-20kg.
    3. LARGE: Generators, engine parts, fridge, desks, bulk sacks (rice/cement), or items > 20kg.

    SPECIAL OVERRIDE: If description contains "generator", "engine", "machine", "bulky", "fridge", "freezer", "table", "chair", or any number followed by "kg" where number > 20, you MUST return LARGE.

    Return ONLY a JSON object: { "size_tier": "SMALL" | "MEDIUM" | "LARGE", "confidence": number }
  `;

  try {
    const result = await model.generateContent(prompt);
    const text = result.response.text();
    const jsonMatch = text.match(/\{.*\}/);
    return JSON.parse(jsonMatch[0]);
  } catch (error) {
    console.error('[Gemini] Classification CRITICAL FAILURE:', {
        message: error.message,
        stack: error.stack,
        description
    });
    return { size_tier: 'MEDIUM', confidence: 0.5 };
  }
};

module.exports = {
  classifyItemSize
};
