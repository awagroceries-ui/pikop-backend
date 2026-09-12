# Task List - Fully In-App Dispatch Flow

- [ ] **Backend: Acknowledgment Engine**
    - [ ] Update `createOrder` logic for `PENDING_ACKNOWLEDGMENT` gating
    - [ ] Implement `acknowledgeOrder` and timeout handling endpoints
    - [ ] Create and start `acknowledgmentReminderJob.js`
- [ ] **Backend: Messaging**
    - [ ] Add `sendAcknowledgmentRequest` to `fcmService.js`
- [ ] **Android: Receiver UI**
    - [ ] Create `OrderAcknowledgmentScreen.kt`
    - [ ] Register route and intent handling in `MainActivity.kt`
- [ ] **Android: Feedback UX**
    - [ ] Add "Incoming Deliveries" alert to `CustomerHomeScreen.kt`
- [ ] **Verification**
    - [ ] Test end-to-end user-to-user flow without SMS
    - [ ] Git automation (Commit and Push)
