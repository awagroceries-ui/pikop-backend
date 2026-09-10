/**
 * Normalizes Nigerian phone numbers to canonical E.164 format (+234...).
 * Handles variations like leading 0, 234, +234, and spaces/dashes.
 */
const normalizePhone = (phone) => {
    if (!phone) return null;

    // 1. Remove all non-numeric characters
    let cleaned = phone.toString().replace(/\D/g, '');

    // 2. Handle Nigerian prefix variants
    if (cleaned.startsWith('0') && cleaned.length === 11) {
        // e.g. 08123456789 -> 2348123456789
        cleaned = '234' + cleaned.substring(1);
    } else if (cleaned.length === 10) {
        // e.g. 8123456789 -> 2348123456789
        cleaned = '234' + cleaned;
    } else if (cleaned.startsWith('234') && cleaned.length === 13) {
        // e.g. 2348123456789 -> stays same
    } else if (cleaned.length < 10) {
        // Too short to be valid, return as is (let DB constraints handle if needed)
        return phone;
    }

    // 3. Prepend '+' for canonical E.164
    return '+' + cleaned;
};

/**
 * Formats a phone number specifically for Termii (Nigerian digits only, no +).
 */
const formatForTermii = (phone) => {
    const normalized = normalizePhone(phone);
    if (!normalized) return null;
    return normalized.replace('+', '');
};

module.exports = { normalizePhone, formatForTermii };
