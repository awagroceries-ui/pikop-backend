# Implementation Plan - Termii SMS Integration & Guest Tracking

This plan outlines the final integration of Termii as the primary SMS provider and the implementation of live guest tracking with automated ₦50 charging.

## User Review Required

> [!IMPORTANT]
> **Termii Sender ID:** I will use `Pikop` as the default sender ID. Please ensure this is registered on your Termii dashboard.
>
> **SMS Charge Policy:** A one-time ₦50 fee is added to any order requiring guest SMS (payment links or tracking links). This is billed to the order's payer and logged for audit.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- Finalize Termii REST client for `sendSms` and `sendOtp`.
- Implement `cost_naira` logging (₦50 for guest messages).
- Use `Pikop` sender ID and the provided API key.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **Bug Fix:** Change `val` to `const` in `resendOtp` cooldown logic.
- Ensure `verifyOtp` correctly resolves Termii's `pin_id`.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Logic Fix:** Update `sms_charge_amount` to apply if **either** the payer or the recipient is a guest (non-app user).
- Ensure `total_payable` correctly sums `item_price + delivery_fee + platform_fee + sms_charge`.

#### [MODIFY] [guest_tracking.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_tracking.ejs)
- Map `primary_class` (`agent`, `rider`, `driver`) to the correct marker icons:
    - `agent` -> `marker_walking.png`
    - `rider` -> `marker_bike.png`
    - `driver` -> `marker_car.png`
- Ensure Leaflet.js uses absolute paths for these markers via the static `/public/assets` route.

#### [MODIFY] [webhookController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/webhookController.js)
- Implement basic security for the Termii delivery report webhook.

---

### Android App

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Polish the Order Summary UI to show a clean "Guest delivery SMS: ₦50" line item only when applicable.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Signup SMS:** Confirm 6-digit OTP arrives via SMS on signup.
2.  **Guest Charge:** Create an order for a guest phone number; verify ₦50 is added to the total.
3.  **Guest Tracking:** Open the tracking link from the SMS on a mobile browser; verify the agent marker moves live.
4.  **Audit Log:** Verify `sms_logs` table shows ₦50 cost for guest messages and ₦0 for signup OTPs.
