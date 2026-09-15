# Walkthrough: Merchant COD Opt-In/Opt-Out

## Changes Made

### 1. Database & Schema
- Added a new migration `1726410000000_add_merchant_cod_toggle.js` which adds the `accepts_cod` BOOLEAN column to both `vendors` and `kitchens` tables.
- Defaulted the value to `true` for all existing and new records.

### 2. Onboarding Update
- Updated `MerchantBusinessSetupScreen.kt` (Stage 2 of onboarding) to include a visual toggle for "Accept COD Orders".
- Included a helpful description explaining that funds are held in escrow and released after delivery, and that the platform fee is customer-borne.

### 3. Business Settings
- Added a fourth tab "Settings" to the `MerchantPortalScreen.kt`.
- Merchants can now toggle their COD preference at any time from within their dedicated module.
- Implemented `PATCH /api/v1/merchants/settings` on the backend to handle these updates.

### 4. Checkout Enforcement
- Updated the `getDiscovery` API in `commerceController.js` to return the `accepts_cod` flag for every product and meal.
- Updated `CommerceCheckoutScreen.kt` to inspect this flag. If a merchant has disabled COD, the "Pay on Delivery" option is automatically hidden from the customer, leaving only "Pay Now" as a valid choice.

## Verification Results
- **Android Build**: Successfully compiled (`:app:assembleDebug`).
- **Data Integrity**: Verified that the `SetupMerchantRequest` and `MerchantProfile` DTOs correctly sync the `accepts_cod` value between the app and the server.
- **Git State**: All changes committed and pushed to `main`.

## Deployment Instructions
To apply these changes to your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
