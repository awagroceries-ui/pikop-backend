# Implementation Plan - Fix Active Order Transmission & Audible Pings

This plan addresses the issue where new mission offers are not actively reaching fulfillers, and implements audible notifications for new requests and periodic reminders.

## Problem Description
1.  **Passive Dispatch:** The system relies on fulfillers to "ask" for new missions (polling) every 60 seconds. It is not actively "pushing" new orders to them the moment they are created.
2.  **Silent Notifications:** The current push notification system sends data-only messages which do not trigger an audible alert or vibration on many Android devices when the app is backgrounded.
3.  **No Reminders:** There is no mechanism to re-alert agents about missions that remain unaccepted.
4.  **Strict Filtering:** The current dispatch filters (State, Class) might be too restrictive or inconsistent, preventing missions from being transmitted to eligible agents.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [fcmService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/fcmService.js)
- **Audible Payload:** Update `sendNotification` to include a `notification` block (Title/Body) alongside the `data` block.
- **Priority:** Ensure `android.priority` is set to `"high"` and `notification.sound` is `"default"`.

#### [MODIFY] [dispatchService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/dispatchService.js)
- **Hardened Search:** Update `findNearbyFulfillers` to:
    - Use `ANY(required_fulfiller_classes)` to match order size.
    - Check Okada-restricted zones using `ST_Intersects`.
    - Use a case-insensitive `ILIKE` or similar for state matching to avoid "Lagos" vs "Lagos State" issues.
- **Enhanced Broadcast:** Update `broadcastOffer` to use the improved `sendNotification` for audible alerts.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Active Trigger:** Call `dispatchService.findNearbyFulfillers` and `broadcastOffer` immediately after a Buyer-initiated mission is created.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **Active Trigger:** Call `dispatchService` inside the webhook handler when a mission status moves to `SEARCHING` after payment.

#### [NEW] [dispatchReminderJob.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/jobs/dispatchReminderJob.js)
- **The "Nudge" Worker:** A job running every 3 minutes that re-broadcasts unaccepted `SEARCHING` missions to nearby eligible fulfillers.

---

### Android App

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- **Socket Connectivity:** Ensure `SocketManager.connect(userId)` is called as soon as the session is loaded and the user is logged in.

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- **Instant Offer Update:** Add a listener for the `new_mission_offer` socket event to refresh the `offers` list immediately, bypassing the 60s poll timer.

#### [MODIFY] [PikopMessagingService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/PikopMessagingService.kt)
- **Notification Channel:** Ensure the `pikop_notifications` channel is configured with `NotificationManager.IMPORTANCE_HIGH` for audible sound.

---

## Verification Plan

### Automated Tests
- Syntax check backend.
- Build Android app.

### Manual Verification
1.  **Immediate Notification:** Create a mission. Verify an online fulfiller sees it **within 3 seconds**.
2.  **Audible Alert:** Put app in background. Create mission. Verify device rings/vibrates.
3.  **Reminder Nudge:** Leave a mission unaccepted for 3 minutes. Verify nearby fulfillers get a reminder notification.
4.  **Class Filter:** Verify a Driver does NOT get a notification for a "Small" item if they don't have Agent/Rider secondary classes.
