const axios = require('axios');
const db = require('../config/db');
const { formatForTermii } = require('../utils/phone');
require('dotenv').config();

const TERMII_API_KEY = (process.env.TERMII_API_KEY || 'tlv_vNooxh-VZNQ4yFmywjNwA5DxC1KdgDkLZYRXOHqtkys').trim();
const TERMII_SENDER_ID = (process.env.TERMII_SENDER_ID || 'N-Alert').trim(); // Default to N-Alert while 'Pikop' is pending
const TERMII_BASE_URL = 'https://api.ng.termii.com/api';

console.log(`[SMS] Service Initialized. Sender ID: ${TERMII_SENDER_ID}`);

/**
 * Common headers for Termii API requests.
 */
const TERMII_HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
};

/**
 * Logs every SMS for audit and cost tracking.
 */
const logSms = async (recipient, content, purpose, orderId = null, ref = null, status = 'sent') => {
    try {
        const cost = (purpose === 'signup_otp') ? 0 : 50; // Signup OTP is free for user, others cost 50 NGN
        await db.query(
            `INSERT INTO sms_logs (recipient, content, purpose, order_id, provider_ref, status, cost_naira)
             VALUES ($1, $2, $3, $4, $5, $6, $7)`,
            [recipient, content, purpose, orderId, ref, status, cost]
        );
    } catch (e) {
        console.error('[SMS] Logging failed:', e.message);
    }
};

/**
 * Generic SMS Send via Termii.
 */
const sendSms = async (to, message, purpose = 'generic', orderId = null) => {
    const termiiPhone = formatForTermii(to);
    try {
        const payload = {
            to: termiiPhone,
            from: TERMII_SENDER_ID,
            sms: message,
            type: "plain",
            channel: "dnd", // Use DND for better reliability in Nigeria
            api_key: TERMII_API_KEY
        };

        const response = await axios.post(`${TERMII_BASE_URL}/sms/send`, payload, { headers: TERMII_HEADERS });
        const ref = response.data.message_id;

        await logSms(to, message, purpose, orderId, ref, 'sent');
        console.log(`[Termii] SMS success for ${to}. Ref: ${ref} | Msg: ${response.data.message}`);

        return { success: true, ref };
    } catch (error) {
        const errorData = error.response?.data || error.message;
        console.error(`[Termii] CRITICAL FAILURE for ${to}:`, JSON.stringify(errorData));

        // ACTIONABLE DIAGNOSTIC for Admin
        if (JSON.stringify(errorData).includes("Country Inactive")) {
            console.error("🚨 PIKOP SYSTEM ALERT: Termii account has not activated SMS delivery to Nigeria (+234) for this route. Please visit your Termii Dashboard -> Coverage to activate.");
        }

        await logSms(to, message, purpose, orderId, null, 'failed');
        return { success: false, error: JSON.stringify(errorData) };
    }
};

/**
 * Sends and Manages OTP via Termii.
 */
const sendOtp = async (to) => {
    const termiiPhone = formatForTermii(to);
    try {
        const payload = {
            api_key: TERMII_API_KEY,
            message_type: "NUMERIC",
            to: termiiPhone,
            from: TERMII_SENDER_ID,
            channel: "dnd", // Use DND for reliable OTP delivery
            pin_attempts: 3,
            pin_time_to_live: 10, // 10 minutes
            pin_length: 6,
            pin_placeholder: "< 1234 >",
            message_text: "Your Pikop verification code is < 1234 >. Valid for 10 minutes."
        };

        const response = await axios.post(`${TERMII_BASE_URL}/sms/otp/send`, payload, { headers: TERMII_HEADERS });

        // Termii response format check
        if (response.data.pinId || response.data.status === 200 || response.data.message === "Successfully Sent") {
            const pinId = response.data.pinId || "manual_v3_ref";
            await logSms(to, "OTP_HIDDEN", "signup_otp", null, pinId, 'sent');
            console.log(`[Termii] OTP success for ${to}. pinId: ${pinId}`);
            return { success: true, pinId };
        } else {
            console.error(`[Termii] OTP unexpected response body:`, JSON.stringify(response.data));
            return { success: false, error: response.data.message };
        }

    } catch (error) {
        const errorData = error.response?.data || error.message;
        console.error(`[Termii] OTP API FAILURE for ${to}:`, JSON.stringify(errorData));
        return { success: false, error: JSON.stringify(errorData) };
    }
};

/**
 * Verifies an OTP with Termii.
 */
const verifyOtpToken = async (pinId, pin) => {
    try {
        const payload = {
            api_key: TERMII_API_KEY,
            pin_id: pinId,
            pin: pin
        };

        const response = await axios.post(`${TERMII_BASE_URL}/sms/otp/verify`, payload);
        return response.data.verified === true;
    } catch (error) {
        console.error(`[Termii] OTP Verification Error:`, error.response?.data || error.message);
        return false;
    }
};

/**
 * Sends a Secure Pay payment link to a guest recipient.
 */
const sendSecurePaySms = async (phone, amount, orderId) => {
    const paymentLink = `https://pay.pikop.com.ng/guest/checkout/${orderId}`;
    const message = `Pikop: You have a Protected Delivery request for ₦${amount}. Pay securely here to activate: ${paymentLink}`;

    return await sendSms(phone, message, 'payment_link', orderId);
};

/**
 * Sends a Live Tracking link to a guest recipient.
 */
const sendTrackingLinkSms = async (phone, orderId) => {
    const trackingLink = `https://track.pikop.com.ng/guest/${orderId}`;
    const message = `Pikop: Your delivery is on the way! Track the agent live at: ${trackingLink}`;

    return await sendSms(phone, message, 'tracking_link', orderId);
};

module.exports = {
    sendSms,
    sendOtp,
    verifyOtpToken,
    sendSecurePaySms,
    sendTrackingLinkSms
};
