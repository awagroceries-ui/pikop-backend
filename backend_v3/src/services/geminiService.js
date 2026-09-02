const { GoogleGenerativeAI } = require("@google/generative-ai");

const API_KEY = process.env.GEMINI_API_KEY;
const genAI = new GoogleGenerativeAI(API_KEY);

/**
 * Classifies an item into a size tier using v3 stable AI.
 * Enhanced for weight and bulky item detection with multi-model fallback.
 */
const classifyItemSize = async (description) => {
  // 0. KEYWORD FALLBACK (Pikop Priority Shield)
  // Ensures common Nigerian bulky items are caught even if AI fails or throttles.
  const desc = (description || '').toLowerCase();
  if (desc.includes('generator') || desc.includes('engine') || desc.includes('fridge') || desc.includes('freezer') || desc.includes('table') || desc.includes('chair') || desc.includes('bulk') || desc.includes('sack')) {
      console.log('[Gemini] Fallback Triggered: LARGE item detected via keywords.');
      return { size_tier: 'LARGE', confidence: 1.0 };
  }
  if (desc.includes('envelope') || desc.includes('key') || desc.includes('document') || desc.includes('food')) {
      return { size_tier: 'SMALL', confidence: 1.0 };
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
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
    const result = await model.generateContent(prompt);
    const text = result.response.text();
    const jsonMatch = text.match(/\{.*\}/);
    return JSON.parse(jsonMatch[0]);
  } catch (error) {
    console.warn('[Gemini] gemini-1.5-flash failed, attempting gemini-pro fallback:', error.message);
    try {
      const fallbackModel = genAI.getGenerativeModel({ model: "gemini-pro" });
      const result = await fallbackModel.generateContent(prompt);
      const text = result.response.text();
      const jsonMatch = text.match(/\{.*\}/);
      return JSON.parse(jsonMatch[0]);
    } catch (fallbackError) {
      console.error('[Gemini] All AI models failed. Defaulting to MEDIUM:', fallbackError.message);
      return { size_tier: 'MEDIUM', confidence: 0.5 };
    }
  }
};

module.exports = {
  classifyItemSize
};
