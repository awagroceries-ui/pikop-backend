# Implementation Plan - Fix Order Activation & Payment Redirection

This plan addresses the "blank page" redirection issue and the failure of order creation after successful payment.

## User Review Required

> [!IMPORTANT]
> The current system relies on the backend to redirect the user back to the app using a custom scheme (`pikop://`). Some mobile browsers block these redirects if they are triggered purely via JavaScript. I will implement a more robust redirect page with a clear "Return to App" button as a fallback.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **Metadata Parsing:** Add logic to handle cases where Paystack returns `metadata` as a string instead of an object.
- **Robust Redirection:** Replace the simple script in `handleWebhookGET` with a branded Pikop "Payment Successful" page that:
    - Auto-redirects using `window.location.replace`.
    - Provides a visible "RETURN TO APP" button.
    - Handles both `reference` and `trxref` parameters.
- **Activation Safety:** Ensure `activatePaidMission` handles numeric conversion for all fee/price fields and wraps external services (FCM/Email) in try-catch.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Constraint Alignment:** Verify that all required fields for a V3 order (like `item_price`) are correctly populated during manual creation to avoid database constraint violations.

#### [MODIFY] [placesController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/placesController.js)
- **Key Safety:** Add a check for `GOOGLE_API_KEY` and return a descriptive 500 error if missing, preventing a server crash.

---

## Verification Plan

### Automated Tests
- Syntax check all modified files using `node -c`.

### Manual Verification
1. **Redirection:** Perform a payment and verify that the "Mission Activated!" screen appears in the browser and successfully returns you to the app.
2. **Order Creation:** Verify that immediately after payment, the mission appears in the Customer and Fulfiller "Missions" tabs.
3. **Search:** Verify that address autocomplete works (confirming the server is stable and key is active).
