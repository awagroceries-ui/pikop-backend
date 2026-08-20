const { GoogleGenerativeAI } = require("@google/generative-ai");

const API_KEY = process.env.GEMINI_API_KEY;
const genAI = new GoogleGenerativeAI(API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" }, { apiVersion: "v1" });

/**
 * Classifies an item into a size tier using v3 stable AI.
 * Enhanced for weight and bulky item detection.
 */
const classifyItemSize = async (description) => {
  if (!API_KEY) return { size_tier: 'MEDIUM', confidence: 0.5 };

  const prompt = `
    Context: You are the logistics classifier for Pikop (a Nigerian delivery app).
    Analyze this item description: "${description}"

    Rules for size_tier (Logistics standard):
    1. SMALL: Envelopes, keys, single small document, very light food items, or items weighing less than 2kg.
    2. MEDIUM: Standard boxes, grocery bags, microwaves, medium luggage, or items weighing 2kg to 20kg.
    3. LARGE: Generators, engine parts, fridge, desks, heavy bulk sacks (rice/cement), or any item weighing over 20kg.

    CRITICAL: If the description contains "kg", "generator", "heavy", "engine", "machine", "bulky", or "big", you MUST classify as LARGE.

    Return ONLY a JSON object: { "size_tier": "SMALL" | "MEDIUM" | "LARGE", "confidence": number }
  `;

  try {
    const result = await model.generateContent(prompt);
    const text = result.response.text();
    const jsonMatch = text.match(/\{.*\}/);
    return JSON.parse(jsonMatch[0]);
  } catch (error) {
    console.warn('[Gemini] Classification failed, falling back to MEDIUM.');
    return { size_tier: 'MEDIUM', confidence: 0.5 };
  }
};

module.exports = {
  classifyItemSize
};
