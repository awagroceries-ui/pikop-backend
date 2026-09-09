const axios = require('axios');
const db = require('../config/db');
require('dotenv').config();

const TERMII_API_KEY = process.env.TERMII_API_KEY || 'tlv_vNooxh-VZNQ4yFmywjNwA5DxC1KdgDkLZYRXOHqtkys';
const TERMII_SENDER_ID = process.env.TERMII_SENDER_ID || 'Pikop';
const TERMII_BASE_URL = 'https://api.ng.termii.com/api';

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
    try {
        const payload = {
            to: to,
            from: TERMII_SENDER_ID,
            sms: message,
            type: "plain",
            channel: "generic",
            api_key: TERMII_API_KEY
        };

        const response = await axios.post(`${TERMII_BASE_URL}/sms/send`, payload);
        const ref = response.data.message_id;

        await logSms(to, message, purpose, orderId, ref, 'sent');
        console.log(`[Termii] SMS sent to ${to}. Ref: ${ref}`);

        return { success: true, ref };
    } catch (error) {
        console.error(`[Termii] Failed to send to ${to}:`, error.response?.data || error.message);
        await logSms(to, message, purpose, orderId, null, 'failed');
        return { success: false, error: error.message };
    }
};

/**
 * Sends and Manages OTP via Termii.
 */
const sendOtp = async (to) => {
    try {
        const payload = {
            api_key: TERMII_API_KEY,
            message_type: "NUMERIC",
            to: to,
            from: TERMII_SENDER_ID,
            channel: "generic",
            pin_attempts: 3,
            pin_time_to_live: 10, // 10 minutes
            pin_length: 6,
            pin_placeholder: "< 1234 >",
            message_text: "Your Pikop verification code is < 1234 >. Valid for 10 minutes."
        };

        const response = await axios.post(`${TERMII_BASE_URL}/sms/otp/send`, payload);
        const pinId = response.data.pinId;

        await logSms(to, "OTP_HIDDEN", "signup_otp", null, pinId, 'sent');
        console.log(`[Termii] OTP generated for ${to}. pinId: ${pinId}`);

        return { success: true, pinId };
    } catch (error) {
        console.error(`[Termii] OTP fail for ${to}:`, error.response?.data || error.message);
        return { success: false, error: error.message };
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
