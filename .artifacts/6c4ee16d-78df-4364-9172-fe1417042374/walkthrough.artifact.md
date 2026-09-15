# Walkthrough: Marketplace Commission

## Changes Made
1. **Configurable Commission Variables**:
   - Updated `backend_v3/src/config/platform.js`. Left the original COD escrow config untouched, but added a new `COMMISSION` object holding the exact rates: Food 10%, Groceries 5%, and Shop 10%.
2. **Order Creation Ledger Tracking**:
   - Added a new database migration (`1726400000000_add_merchant_commission.js`) to add `merchant_commission_amount DECIMAL(12,2)` to the `orders` table.
   - Updated the `initializeCommerceOrder` function in `commerceController.js`. It now checks the `item_type` and `category` to calculate the `merchantCommissionAmount` using the configured percentages.
   - **Crucially**, it still treats the COD platform fee separately (calculated on top, only if COD is selected). Both are stored gracefully on the order!
3. **Escrow Settlement Deductions**:
   - Updated `walletService.releaseEscrow()`. Previously, it credited the seller the full `itemPrice` (minus the escrow fee if the seller strangely opted to pay it). Now, it explicitly deducts the `marketplaceCommission` from the `itemPrice` before crediting the merchant's `available` wallet!
   - This ensures absolute financial accuracy when the fulfiller delivers the order.
4. **Transparent Merchant UI**:
   - Modified both `MerchantOrdersScreen.kt` (for live incoming orders) and `MerchantPortalScreen.kt` (for historical completed sales).
   - Instead of a single "Price" string, the merchant now sees a structured breakdown:
     - **Item Price:** `₦10,000`
     - **Marketplace Commission:** `-₦1,000` (in red text)
     - **Your Net Payout:** `₦9,000` (in bold primary color)
   - This creates total transparency without bothering the buyer's receipt.

## Build and Testing Status
- The Android project compiles smoothly (`:app:assembleDebug`).
- The backend API and DB migrations are stable.
- All code has been successfully committed to version control and pushed to your `main` repository!