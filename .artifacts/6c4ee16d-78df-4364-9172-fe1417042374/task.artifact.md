# Task: Fix Real-Time Chat & Notification Deep-Linking

- [x] **Part 1: Real-Time Chat Updates**
    - [x] Update `ChatScreen.kt` to explicitly join rooms on entry
- [x] **Part 2: Backend Notification Hardening**
    - [x] Update `fcmService.js` to include metadata in all notifications
    - [x] Update `socketService.js` to pass chat IDs to FCM
- [x] **Part 3: Android Notification Handling**
    - [x] Update `PikopMessagingService.kt` to extract `conversation_id` and handle key variations
    - [x] Update `MainActivity.kt` routing logic for `SUPPORT_CHAT` and `ORDER_UPDATE`
- [ ] **Part 4: Verification**
    - [ ] Test live chat updates (no refresh)
    - [ ] Test cold-start deep-linking
    - [ ] Test warm-start deep-linking
    - [x] Git commit and push
