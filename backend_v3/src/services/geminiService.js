const { GoogleGenerativeAI } = require("@google/generative-ai");

const API_KEY = process.env.GEMINI_API_KEY;
const genAI = new GoogleGenerativeAI(API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" }, { apiVersion: "v1" });

/**
 * Classifies an item into a size tier using v3 stable AI.
 */
const classifyItemSize = async (description) => {
  if (!API_KEY) return { size_tier: 'MEDIUM', confidence: 0.5 };

  const prompt = `
    Analyze this delivery description: "${description}"
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
