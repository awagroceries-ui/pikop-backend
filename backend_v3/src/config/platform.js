/**
 * Platform-wide configurations for Fees and Escrow.
 */
const PlatformConfig = {
    // Escrow & Secure Pay (Buyer borne)
    ESCROW: {
        FEE_PERCENTAGE: 0.10, // 10%
        GRACE_PERIOD_HOURS: 48,
        REMINDER_BEFORE_RELEASE_HOURS: 4,
    },

    // Marketplace Commission (Seller borne)
    COMMISSION: {
        FOOD_PERCENTAGE: 0.10,      // 10%
        GROCERIES_PERCENTAGE: 0.05, // 5%
        SHOP_PERCENTAGE: 0.10       // 10%
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
