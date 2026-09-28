const axios = require('axios');
const qs = require('querystring');
const db = require('../config/db');
const { normalizePhone } = require('../utils/phone');
require('dotenv').config();

const AT_USERNAME = (process.env.AT_USERNAME || process.env.AFRICASTALKING_USERNAME || 'pikop').trim();
const AT_API_KEY = (process.env.AT_API_KEY || process.env.AFRICASTALKING_API_KEY || '').trim();
const AT_SENDER_ID = (process.env.AT_SENDER_ID || 'Pikop').trim(); // Approved Sender ID

const AT_BASE_URL = AT_USERNAME === 'sandbox'
    ? 'https://api.sandbox.africastalking.com/version1/messaging'
    : 'https://api.africastalking.com/version1/messaging';

console.log(`[SMS] Service Initialized via Africa's Talking. Sender: ${AT_SENDER_ID} | Username: ${AT_USERNAME}`);

/**
 * Format phone number for Africa's Talking (+234 format).
 */
const formatForAT = (phone) => {
    if (!phone) return '';
    const clean = phone.replace(/\D/g, '');
    if (clean.startsWith('234')) return `+${clean}`;
    if (clean.startsWith('0')) return `+234${clean.slice(1)}`;
    return `+${clean}`;
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
 * Generic SMS Send via Africa's Talking.
 */
const sendSms = async (to, message, purpose = 'generic', orderId = null) => {
    const formattedPhone = formatForAT(to);
    try {
        const postData = qs.stringify({
            username: AT_USERNAME,
            to: formattedPhone,
            message: message,
            from: AT_SENDER_ID
        });

        const response = await axios.post(AT_BASE_URL, postData, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded',
                'apiKey': AT_API_KEY
            },
            timeout: 10000
        });

        const recipients = response.data?.SMSMessageData?.Recipients || [];
        const recipientData = recipients[0] || {};
        const status = recipientData.status || 'Success';
        const ref = recipientData.messageId || `AT_${Date.now()}`;

        await logSms(to, message, purpose, orderId, ref, status.toLowerCase().includes('success') ? 'sent' : 'failed');
        console.log(`[AfricaTalking] SMS sent to ${formattedPhone}. Status: ${status} | Ref: ${ref}`);

        return { success: true, ref };
    } catch (error) {
        const errorData = error.response?.data || error.message;
        const errorStr = typeof errorData === 'object' ? JSON.stringify(errorData) : errorData;
        console.error(`[AfricaTalking] SMS Failed for ${to}:`, errorStr);

        await logSms(to, message, purpose, orderId, null, 'failed');
        return { success: false, error: errorStr };
    }
};

/**
 * Sends OTP code via Africa's Talking SMS.
 */
const sendOtp = async (to) => {
    const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
    const message = `Your Pikop verification code is ${otpCode}. Valid for 10 minutes.`;

    const res = await sendSms(to, message, 'signup_otp');
    if (res.success) {
        return { success: true, pinId: res.ref, otpCode };
    }
    return { success: false, error: res.error };
};

/**
 * Verifies OTP token against internal database.
 */
const verifyOtpToken = async (userId, pin) => {
    try {
        const { rows } = await db.query(
            "SELECT id FROM otp_verifications WHERE user_id = $1 AND otp_code = $2 AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1",
            [userId, pin]
        );
        return rows.length > 0;
    } catch (error) {
        console.error(`[SMS] OTP Verification Error:`, error.message);
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

/**
 * Sends an immediate alert to a guest receiver when a mission is created.
 */
const sendNewDeliveryAlert = async (phone, senderName, orderId) => {
    const trackingLink = `https://track.pikop.com.ng/guest/${orderId}`;
    const message = `Pikop: ${senderName} sent you a package! Track it live here: ${trackingLink}`;

    return await sendSms(phone, message, 'incoming_delivery', orderId);
};

module.exports = {
    sendSms,
    sendOtp,
    verifyOtpToken,
    sendSecurePaySms,
    sendTrackingLinkSms,
    sendNewDeliveryAlert
};
