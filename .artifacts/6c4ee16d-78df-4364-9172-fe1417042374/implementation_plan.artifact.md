# Implementation Plan - Real-Time Chat & Notification Deep-Linking Fixes

This plan addresses the real-time message delivery issues in chat and the inconsistent behavior when tapping push notifications.

## Proposed Changes

### 1. Real-Time Chat Updates

#### [MODIFY] [ChatScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/chat/ChatScreen.kt)
- Update the `DisposableEffect` to explicitly emit a join event (`join_order` or `join_support`) when the screen is entered. This ensures the socket is in the correct room even if the mission/conversation was created after the initial socket connection.
- Ensure the `SocketManager` is correctly listening for `receive_message` and updating the local `messages` state.

### 2. Notification Deep-Linking Restoration

#### [MODIFY] [fcmService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/fcmService.js) (Backend)
- Standardize payload keys to match what the Android app expects.
- Update `sendNotification` to always include `type` and relevant IDs (`order_id`, `conversation_id`).
- Ensure support chat notifications (triggered in `socketService.js`) include `type: "SUPPORT_CHAT"` and `conversation_id`.

#### [MODIFY] [PikopMessagingService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/PikopMessagingService.kt)
- Robustly check for both snake_case (`order_id`) and camelCase (`orderId`) keys in the FCM data payload to ensure backward and forward compatibility.
- Add support for `conversation_id` extraction.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Enhance the `LaunchedEffect(incomingIntent)` block to handle `SUPPORT_CHAT` navigation more reliably.
- Map `ORDER_UPDATE` to the correct screens depending on whether the user is a Fulfiller or Customer.

## User Review Required

> [!IMPORTANT]
> **Payload Synchronization**
> I am standardizing the notification payload to use `order_id` and `conversation_id` (snake_case) to align with existing backend patterns. The Android app will be updated to look for these specifically.

## Verification Plan

### Manual Verification
1.  **Live Chat**: Open Chat on Account A and Account B. Send a message from A. Verify it appears on B's screen instantly without refreshing.
2.  **Cold Start Deep-link**: Kill the app. Send a chat message to the user. Tap the notification. Verify the app opens directly to the chat conversation.
3.  **Warm Start Deep-link**: Put the app in the background. Send a chat message. Tap the notification. Verify the app brings the chat conversation to the foreground.
4.  **Order Update Deep-link**: Trigger an order status update (e.g., mark as picked up). Tap the notification and verify it opens the mission tracking screen.
