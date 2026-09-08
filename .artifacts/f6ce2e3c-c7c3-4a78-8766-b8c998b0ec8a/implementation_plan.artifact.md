# Implementation Plan - Emergency Fix for Mission Completion 500 & Payment Channels

This plan provides deep diagnostic logging and fixes for the persistent issues on the production server.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Wrap `verifyDelivery` in a more verbose error handler that returns the exact error message to the app.
- Ensure all variables used in `verifyDelivery` are properly cast and checked.
- Fix the `UPDATE` query for `grace_period_expires_at` to use a more standard PostgreSQL interval casting.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Re-order `channels` to prioritize `bank_transfer`.
- Add logging to capture the response from Paystack during initialization to see if they are rejecting certain channels.

#### [MODIFY] [placesController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/placesController.js)
- Add extreme logging to capture the full URL and response from Google Places.

---

### Android App

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Update the error Toast to show the `message` field from the 500 JSON response if available, which will now contain the server-side error detail.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification (Diagnostic)
1. **Complete Mission:** When it fails, the Toast should now say "Process Failure: Internal Error: <exact database error>".
2. **Checkout:** Check the VPS logs for `[Paystack] Initialization successful. Channels: ...`.
3. **Address Search:** Check the VPS logs for `[Places] Google Response Status: ...`.
