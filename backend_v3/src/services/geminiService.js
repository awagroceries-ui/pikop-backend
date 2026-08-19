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

    Rules for size_tier:
    1. SMALL: Envelopes, documents, single small food items, keys, or items < 2kg.
    2. MEDIUM: Boxes, multiple grocery bags, microwave-sized items, or items 2kg-15kg.
    3. LARGE: Generators, furniture, appliances, bulk sacks, or items > 15kg.

    Special instruction: If "generator", "engine", "fridge", "table", or weights like "20kg", "50kg" are mentioned, ALWAYS classify as LARGE.

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
