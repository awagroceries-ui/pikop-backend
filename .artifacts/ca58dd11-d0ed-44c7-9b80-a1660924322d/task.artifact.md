# Support Chat Implementation Task List [DONE]

## Phase 1: Database & Schema
- [x] Create `messaging_system` migration (`conversations` and `messages` tables)

## Phase 2: Backend Logic & Socket.io
- [x] Create `supportController.js` (Conversation & Message CRUD)
- [x] Create `supportRoutes.js`
- [x] Update `socketService.js` (Support rooms and real-time broadcasting)
- [x] Update `app.js` to register support routes

## Phase 3: Admin Dashboard
- [x] Update `adminController.js` with Support Inbox logic
- [x] Create `support_inbox.ejs` view
- [x] Create `support_detail.ejs` view (Real-time admin chat)
- [x] Update `layout.ejs` sidebar with "Support Inbox"

## Phase 4: Android App Integration
- [x] Update `ApiService.kt` with support endpoints
- [x] Create `ChatScreen.kt` (Reusable chat UI)
- [x] Integrate "Contact Support" in `AboutPikopScreen.kt`
- [x] Update `MainActivity.kt` navigation

## Verification
- [ ] Test real-time message exchange (User <-> Admin)
- [ ] Verify FCM notifications for offline users
- [ ] Verify automatic reopening of closed conversations
