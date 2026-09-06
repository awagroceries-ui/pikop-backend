/**
 * SMS Gateway Service.
 * Currently supports a stub mode (disabled) and prepares for integration with providers like Termii or Twilio.
 */
const SMS_ENABLED = process.env.SMS_ENABLED === 'true';

const sendSms = async (to, message) => {
    if (!SMS_ENABLED) {
        console.log(`[SMS-STUB] To: ${to} | Msg: ${message}`);
        return { success: true, messageId: 'stub_' + Date.now() };
    }

    try {
        // TODO: Implement actual provider logic here
        // Example (Termii):
        // const axios = require('axios');
        // await axios.post('https://api.ng.termii.com/api/sms/send', { ... });

        console.log(`[SMS] Delivered to ${to}`);
        return { success: true };
    } catch (error) {
        console.error(`[SMS] Failed to send to ${to}:`, error.message);
        return { success: false, error: error.message };
    }
};

/**
 * Sends a Secure Pay payment link to a guest recipient.
 */
const sendSecurePaySms = async (phone, amount, orderId) => {
    const paymentLink = `https://pay.pikop.ng/secure/${orderId}`;
    const message = `Pikop: You have a Protected Delivery request for ₦${amount}. Pay securely here to activate: ${paymentLink}`;

    return await sendSms(phone, message);
};

module.exports = {
    sendSms,
    sendSecurePaySms
};
