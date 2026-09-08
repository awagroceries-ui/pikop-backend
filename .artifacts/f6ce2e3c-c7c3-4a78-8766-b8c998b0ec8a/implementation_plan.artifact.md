# Implementation Plan - Fix Address Autocomplete & Include Bank Transfer in Checkout

## Problem Description

1. **Address Search Autocomplete**: The backend places routes (`/autocomplete`, `/details`) required `authenticateToken`, which could cause autocomplete failures if token headers were missing or delayed during initial address input.
2. **Order Checkout Payment Options**: Removing `channels` entirely resulted in Paystack omitting Bank Transfer. We need to explicitly pass `channels: ['card', 'bank', 'ussd', 'bank_transfer', 'qr', 'mobile_money']` to force Paystack to display Bank Transfer alongside card and USSD.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [placesRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/placesRoutes.js)
- Remove `authenticateToken` middleware from `/autocomplete` and `/details` routes so address search is fully public and lightning-fast.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- In `initializePayment`, include `channels: ['card', 'bank', 'ussd', 'bank_transfer', 'qr', 'mobile_money']` to guarantee Bank Transfer and all options are presented on Paystack checkout.

---

## Verification Plan

### Automated Tests
- Run Android build verification via `./gradlew assembleDebug --no-daemon`.

### Manual Verification
- Test address search autocomplete in the app to ensure live suggestions appear without delay.
- Test order checkout to verify Bank Transfer, Card, and USSD options are available.
