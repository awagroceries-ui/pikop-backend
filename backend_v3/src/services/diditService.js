const axios = require('axios');
const crypto = require('node:crypto');

const DIDIT_API_KEY = process.env.DIDIT_API_KEY;
const DIDIT_WEBHOOK_SECRET = process.env.DIDIT_WEBHOOK_SECRET;
const WORKFLOW_ID = "f718c93d-e9d2-432b-b964-7ebf702eceb8"; // v3 Standard Workflow

/**
 * Creates a new identity verification session.
 */
const createSession = async (userId) => {
  if (!DIDIT_API_KEY) throw new Error('Didit API Key missing');

  try {
    const res = await axios.post("https://verification.didit.me/v3/session/", {
      workflow_id: WORKFLOW_ID,
      vendor_data: userId.toString(),
      callback: "https://api.awa.name.ng/api/v1/fulfillers/kyc/webhook",
    }, {
      headers: {
        "x-api-key": DIDIT_API_KEY,
        "Content-Type": "application/json",
      }
    });

    return res.data;
  } catch (error) {
    console.error('[Didit] Session Error:', error.response?.data || error.message);
    throw new Error('Identity verification service unavailable');
  }
};

/**
 * Verifies the X-Signature-V2 HMAC signature.
 */
const verifyWebhook = (rawBody, signature, timestamp) => {
  if (!DIDIT_WEBHOOK_SECRET) return false;

  const expected = crypto
    .createHmac("sha256", DIDIT_WEBHOOK_SECRET)
    .update(rawBody, "utf8")
    .digest("hex");

  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
};

module.exports = {
  createSession,
  verifyWebhook
};
