/**
 * Platform-wide configurations for Fees and Escrow.
 */
const PlatformConfig = {
    // Escrow & Secure Pay (Buyer borne)
    ESCROW: {
        FEE_PERCENTAGE: 0.05, // 5%
        GRACE_PERIOD_HOURS: 48,
        REMINDER_BEFORE_RELEASE_HOURS: 4,
    },

    // Marketplace Commission (Seller borne) - 5% across all categories
    COMMISSION: {
        FOOD_PERCENTAGE: 0.05,      // 5%
        GROCERIES_PERCENTAGE: 0.05, // 5%
        SHOP_PERCENTAGE: 0.05       // 5%
    },

    DISPATCH_COMMISSION_RATE: 0.20, // 20% platform share / 80% Fulfiller share

    // Rounding Strategy: Down to nearest Naira
    roundFee: (amount) => Math.floor(amount),

    // Roles and Entities
    ROLES: {
        PAYER: 'PAYER',
        SELLER: 'SELLER'
    }
};

module.exports = PlatformConfig;
