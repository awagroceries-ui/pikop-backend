/**
 * Platform-wide configurations for Fees and Escrow.
 */
const PlatformConfig = {
    // Escrow & Secure Pay
    ESCROW: {
        FEE_PERCENTAGE: 0.10, // 10%
        GRACE_PERIOD_HOURS: 48,
        REMINDER_BEFORE_RELEASE_HOURS: 4,
    },

    // Rounding Strategy: Down to nearest Naira
    roundFee: (amount) => Math.floor(amount),

    // Roles and Entities
    ROLES: {
        PAYER: 'PAYER',
        SELLER: 'SELLER'
    }
};

module.exports = PlatformConfig;
