# Implementation Plan - Termii SMS Integration & Guest Tracking

This plan outlines the integration of Termii as the primary SMS provider for signup OTPs, guest payment links, and guest live tracking links, including the automated ₦50 SMS charge.

## User Review Required

> [!IMPORTANT]
> **Termii Sender ID:** I will use `Pikop` as the default sender ID. Please ensure this is registered on your Termii dashboard to avoid carrier filtering in Nigeria.
>
> **SMS Charge Policy:** Every order involving a guest recipient (non-app user) will incur a one-time ₦50 fee added to the `total_payable`. This covers all guest SMS needed for that specific order (payment links + tracking links).

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725592000000_add_termii_and_sms_charge.js)
- Add `phone_verified_at` to `users` table.
- Add `sms_charge_amount` to `quotes` and `orders` tables.
- Create `sms_logs` table to track every SMS sent (recipient, content, purpose, order_id, status, provider_ref).

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- Implement `sendSms` and `sendOtp` using Termii's REST API.
- Add `sendTrackingLinkSms` for guest receivers.
- Implement robust error handling and logging to `sms_logs`.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **`signup`**: Send OTP via SMS (using Termii) in addition to email.
- **`verifyEmail`**: Rename to `verifyOtp` and handle both email and phone verification states.
- **`resendOtp`**: Add 60-second cooldown per user to prevent abuse.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getQuote`**: Calculate `sms_charge_amount` (₦50 if `payer_type === 'GUEST'`) and include in `total_payable`.
- **`createOrder`**: Persist `sms_charge_amount` to the mission record.
- **`updateStatus`**: Automatically trigger `sendTrackingLinkSms` when a guest-bound mission moves to `PICKED_UP`.

#### [NEW] [guestTracking.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_tracking.ejs)
- A lightweight, public EJS view with Leaflet.js and Socket.io.
- Reuses the `location_updated` socket stream for real-time movement.
- Displays mission status and ETA without requiring an app login.

#### [MODIFY] [webhookController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/webhookController.js)
- **`handleTermiiWebhook` [NEW]**: Process delivery reports from Termii and update `sms_logs`.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `QuoteResponse` and `OrderDetailsResponse` to include `sms_charge_amount`.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Update the Order Summary UI to show "Guest SMS Charge: ₦50" when applicable.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Signup:** Register a new account and confirm OTP arrives via SMS.
2.  **Guest Order:** Create an order for a guest. Verify the ₦50 charge appears in the summary.
3.  **Guest Tracking:** mark a guest order as `PICKED_UP`. Open the link in the SMS and verify the real-time map works.
4.  **Webhook:** Trigger a test delivery report from Termii and verify `sms_logs` status updates.
