const axios = require('axios');
const crypto = require('node:crypto');
require('dotenv').config();

const DIDIT_API_KEY = process.env.DIDIT_API_KEY;
const DIDIT_WEBHOOK_SECRET = process.env.DIDIT_WEBHOOK_SECRET;
const WORKFLOW_ID = "f718c93d-e9d2-432b-b964-7ebf702eceb8"; // Free KYC

/**
 * Creates a new Didit verification session.
 * @param {number} userId - Internal user ID to link with session.
 */
const createSession = async (userId) => {
  try {
    const res = await axios.post("https://verification.didit.me/v3/session/", {
      workflow_id: WORKFLOW_ID,
      vendor_data: userId.toString(),
      callback: "https://api.awa.name.ng/api/v1/fulfillers/kyc/done", // Placeholder for return redirect
    }, {
      headers: {
        "x-api-key": DIDIT_API_KEY,
        "Content-Type": "application/json",
      }
    });

    return res.data; // { session_id, session_token, url, ... }
  } catch (error) {
    console.error('Didit Session Creation Error:', error.response?.data || error.message);
    throw new Error('Failed to initialize identity verification session');
  }
};

/**
 * Canonicalizes data for HMAC verification.
 */
function shortenFloats(v) {
  if (Array.isArray(v)) return v.map(shortenFloats);
  if (v && typeof v === "object") {
    return Object.fromEntries(
      Object.entries(v).map(([k, x]) => [k, shortenFloats(x)]),
    );
  }
  if (typeof v === "number" && !Number.isInteger(v) && v % 1 === 0) return Math.trunc(v);
  return v;
}

function sortKeys(v) {
  if (Array.isArray(v)) return v.map(sortKeys);
  if (v && typeof v === "object") {
    return Object.keys(v)
      .sort()
      .reduce((acc, k) => {
        acc[k] = sortKeys(v[k]);
        return acc;
      }, {});
  }
  return v;
}

/**
 * Verifies the X-Signature-V2 HMAC signature on Didit webhooks.
 */
const verifyWebhookSignature = (rawBody, signature, timestamp) => {
  // 1. Freshness check (300s)
  if (!timestamp || Math.abs(Date.now() / 1000 - Number(timestamp)) > 300) {
    return false;
  }

  // 2. Canonicalize
  const parsed = JSON.parse(rawBody);
  const canonical = JSON.stringify(sortKeys(shortenFloats(parsed)));

  // 3. Compare HMAC
  const expected = crypto
    .createHmac("sha256", DIDIT_WEBHOOK_SECRET)
    .update(canonical, "utf8")
    .digest("hex");

  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
};

module.exports = {
  createSession,
  verifyWebhookSignature
};
