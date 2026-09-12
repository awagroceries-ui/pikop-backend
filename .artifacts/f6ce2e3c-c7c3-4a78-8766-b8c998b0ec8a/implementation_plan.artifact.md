# Implementation Plan - Fully In-App User-to-User Dispatch Flow

This plan implements a professional, secure, and fully in-app dispatch flow for missions between two registered Pikop users, ensuring explicit receiver acknowledgment before any fulfiller is dispatched.

## User Review Required

> [!IMPORTANT]
> **Dispatch Gating:** Missions between two app users will now enter a `PENDING_ACKNOWLEDGMENT` status. They will **not** be visible to fulfillers until the receiver explicitly confirms they are expecting the item and verifies the delivery address.
>
> **No-Response Resolution:** If a receiver ignores the request for 2 hours, the sender will be given the choice to "Proceed anyway," "Keep waiting," or "Cancel."

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`createOrder`**:
    - Detect if `recipient_user_id` is present.
    - If so, set initial status to `PENDING_ACKNOWLEDGMENT` instead of `SEARCHING`.
    - Skip all SMS/Guest triggers.
    - Call `fcmService.sendAcknowledgmentRequest(recipient_user_id, orderId)`.
- **`acknowledgeOrder` [NEW]**:
    - Endpoint for User B to `confirm` (moves to `SEARCHING` or `PAYMENT_CAPTURED`) or `decline` (moves to `CANCELLED`).
    - Allows User B to provide a `corrected_address` during confirmation.
- **`handleAcknowledgmentTimeout` [NEW]**:
    - Logic for User A to decide next steps after the 2-hour timeout.

#### [NEW] [acknowledgmentReminderJob.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/jobs/acknowledgmentReminderJob.js)
- Background worker running every 15 minutes.
- Sends a "Gentle Reminder" push to User B if unacknowledged for > 30 mins.
- Sends a "Timeout Choice" push to User A if unacknowledged for > 2 hours.

#### [MODIFY] [fcmService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/fcmService.js)
- Add `sendAcknowledgmentRequest`: High-priority alert for User B.
- Add `sendAcknowledgmentTimeoutAlert`: Prompt for User A choice.

---

### Android App

#### [NEW] [OrderAcknowledgmentScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderAcknowledgmentScreen.kt)
- Screen for User B to view incoming request details.
- **Actions:** "Confirm & Verify Address," "Change Delivery Address," "I wasn't expecting this."

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add `ACKNOWLEDGMENT_REQUEST` to the `navigateTo` intent handler.
- Register the new `order_acknowledgment/{orderId}` route.

#### [MODIFY] [CustomerHomeScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/CustomerHomeScreen.kt)
- Add an "Incoming Deliveries" section or badge to alert the user of pending acknowledgments.

---

## Verification Plan

### Manual Verification
1.  **Direct Flow:** User A sends to User B. Verify **zero SMS** are sent. Verify User B receives a push notification.
2.  **Acknowledgment Gate:** Verify the mission remains `PENDING_ACKNOWLEDGMENT` and is **invisible to fulfillers** until User B clicks "Confirm."
3.  **Address Correction:** User B changes the delivery address during acknowledgment. Verify the mission updates and fulfillers see the *new* address.
4.  **Timeout Handling:** Wait 2 hours (or simulate). Verify User A is prompted to "Proceed anyway" and can force the mission into the fulfiller queue.
