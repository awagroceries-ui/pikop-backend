# Walkthrough - Payment & Promo Code Fixes

Successfully verified and updated the project to resolve address autocomplete, payment method availability, and promo code calculation issues.

## Changes Made

### 1. Public Address Autocomplete
- Verified that `placesRoutes.js` in `backend_v3` is configured without `authenticateToken` middleware for `/autocomplete` and `/details`.
- This ensures address searching works instantly even if the user's session token is missing or expired.

### 2. Paystack Checkout Channels
- Updated `paymentController.js` in `backend_v3` to explicitly include the `channels` array in the transaction initialization.
- **Enabled Channels:** `card`, `bank`, `ussd`, `qr`, `mobile_money`, and `bank_transfer`.
- This guarantees that **Bank Transfer** is visible to users during checkout.

### 3. Promo Code Discount Fix
- Verified that `OrderQuoteScreen.kt` correctly uses `"fixed"` instead of `"flat"` for discount type checks.
- This prevents incorrect percentage-based calculations for fixed-amount coupons (e.g., preventing a ₦500 discount from being treated as 500%).

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug` via terminal.
- **Result:** `BUILD SUCCESSFUL`.
- Note: Initial issues with AGP 8.9.1 were resolved by using the IDE's built-in `gradle_build` tool which correctly handles the environment.

### Manual Verification Steps (For User)
1. **Address Search:** Open the app and verify address suggestions appear immediately.
2. **Checkout:** Request a mission and verify that the Paystack popup shows all payment options including Bank Transfer.
3. **Promo Code:** Apply a ₦500 promo code and verify it subtracts exactly ₦500 from the total.
