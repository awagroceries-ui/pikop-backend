const axios = require('axios');
const crypto = require('node:crypto');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const DIDIT_API_KEY = process.env.DIDIT_API_KEY;
const DIDIT_WEBHOOK_SECRET = process.env.DIDIT_WEBHOOK_SECRET;
const WORKFLOW_ID = "f718c93d-e9d2-432b-b964-7ebf702eceb8"; // Free KYC

// Diagnostic check on startup
if (!DIDIT_API_KEY) {
    console.error('❌ DIDIT_API_KEY is missing from .env');
} else {
    console.log('✅ Didit Service: API Key detected.');
}

/**
 * Creates a new Didit verification session.
 * @param {number} userId - Internal user ID to link with session.
 */
const createSession = async (userId) => {
  if (!DIDIT_API_KEY) {
    throw new Error('Didit API Key is not configured on the server.');
  }

  try {
    console.log(`[Didit] Initializing session for User ${userId} with Workflow ${WORKFLOW_ID}`);
    const res = await axios.post("https://verification.didit.me/v3/session/", {
      workflow_id: WORKFLOW_ID,
      vendor_data: userId.toString(),
      callback: "https://api.awa.name.ng/api/v1/fulfillers/kyc/done",
    }, {
      headers: {
        "x-api-key": DIDIT_API_KEY,
        "Content-Type": "application/json",
      }
    });

    console.log(`[Didit] Session created: ${res.data.session_id}`);
    return res.data;
  } catch (error) {
    const detail = error.response?.data?.detail || error.message;
    console.error('Didit Session Creation Error:', detail);
    if (error.response) {
      console.error('[Didit] Status:', error.response.status);
      console.error('[Didit] Response Data:', JSON.stringify(error.response.data));
    }
    throw new Error(`Didit Error: ${detail}`);
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
  if (!DIDIT_WEBHOOK_SECRET) {
    console.error('❌ DIDIT_WEBHOOK_SECRET is missing. Webhook verification failed.');
    return false;
  }

  // 1. Freshness check (300s)
  if (!timestamp || Math.abs(Date.now() / 1000 - Number(timestamp)) > 300) {
    console.warn('⚠️ Webhook timestamp freshness check failed.');
    return false;
  }

  try {
    // 2. Canonicalize
    const parsed = JSON.parse(rawBody);
    const canonical = JSON.stringify(sortKeys(shortenFloats(parsed)));

    // 3. Compare HMAC
    const expected = crypto
      .createHmac("sha256", DIDIT_WEBHOOK_SECRET)
      .update(canonical, "utf8")
      .digest("hex");

    return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
  } catch (e) {
    console.error('Webhook Canonicalization Error:', e.message);
    return false;
  }
};

module.exports = {
  createSession,
  verifyWebhookSignature
};
