const { GoogleGenerativeAI } = require("@google/generative-ai");
require('dotenv').config();

// Force use of stable v1 API to avoid v1beta 404 errors
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" }, { apiVersion: "v1" });

/**
 * Classifies an item description into a size tier.
 * Returns { size_tier: 'SMALL' | 'MEDIUM' | 'LARGE', confidence: number }
 */
const classifyItemSize = async (description) => {
  const prompt = `
    Analyze the following delivery item description and classify it into one of these three size tiers:
    - SMALL: Fits in an envelope or small bag (e.g., documents, keys, single meal, small electronics like a phone).
    - MEDIUM: Fits in a standard shoe box or backpack (e.g., clothes, multiple meals, larger electronics like a laptop, shoe box).
    - LARGE: Requires a car or large storage box (e.g., groceries, multiple shoe boxes, small appliances).

    Return ONLY a JSON object with the following structure:
    { "size_tier": "SMALL" | "MEDIUM" | "LARGE", "confidence": number (between 0 and 1) }

    Description: "${description}"
  `;

  try {
    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    // Extract JSON from response (handling potential markdown formatting)
    const jsonMatch = text.match(/\{.*\}/);
    if (!jsonMatch) throw new Error("Could not parse AI response");

    const classification = JSON.parse(jsonMatch[0]);
    return classification;
  } catch (error) {
    console.error("Gemini Classification Error:", error);
    // Default to LARGE for safety if classification fails
    return { size_tier: 'LARGE', confidence: 0.5 };
  }
};

module.exports = {
  classifyItemSize
};
