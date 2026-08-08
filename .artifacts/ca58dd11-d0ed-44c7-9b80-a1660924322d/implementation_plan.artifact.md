# Implementation Plan - Support Chat & Messaging Infrastructure

Implement a native real-time Support Chat system for Pikop, extending the Socket.io infrastructure and replacing the previous third-party (Tawk.to) integration plan.

## User Review Required

> [!IMPORTANT]
> - **Unified Messaging**: I will implement a single `messages` table that handles both Order-based chat and Support-based conversations.
> - **Real-time Engine**: I will reuse the existing Socket.io setup, adding specialized rooms for support threads (`support:{id}`).
> - **Admin Attribution**: Admins will be able to reply directly from the dashboard, with their messages clearly identified as "Admin" in the app.

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1722970000000_messaging_system.js`
- **Conversations**: `id` (uuid), `participant_type` (user/fulfiller), `participant_id` (int, references users/fulfillers), `status` (open/closed), `created_at`, `last_message_at`.
- **Messages**: `id` (uuid), `conversation_id` (FK, nullable), `order_id` (FK, nullable), `sender_id` (int), `sender_type` (user/fulfiller/admin), `content` (text), `created_at`.
- **Constraint**: Ensure exactly one of `conversation_id` or `order_id` is present.

---

### 2. Support Logic & Real-time (Backend)

#### [NEW] `backend/src/controllers/supportController.js`
- `getOrCreateConversation`: Logic to find an existing open support thread or start a new one.
- `getMessages`: Paginated retrieval of chat history.

#### [MODIFY] `backend/src/services/socketService.js`
- Add `join_support` room handler.
- Implement `send_message` event:
    - Persists message to DB.
    - Broadcasts to the specific room (order or support).
    - Triggers FCM notification if the recipient is not currently connected to the room.

#### [NEW] `backend/src/routes/supportRoutes.js`
- Endpoints for Participant usage: `POST /`, `GET /:id/messages`.

---

### 3. Admin Command Center (UI & API)

#### [MODIFY] `backend/src/controllers/adminController.js`
- `getSupportInbox`: List all open conversations with participant names and previews.
- `replyToSupport`: API to send messages as 'admin'.
- `resolveSupport`: Mark conversation as closed.

#### [NEW] `backend/src/views/support_inbox.ejs`
- A shared inbox view for the Support team.
- Real-time message updates using Socket.io client.

---

### 4. Android UI Integration

#### [NEW] `ChatScreen.kt`
- A reusable Compose component for a standard chat interface.
- Handles real-time Socket.io events and message history.

#### [MODIFY] `AboutPikopScreen.kt` (Help Center)
- Add "Contact Support" button which launches `ChatScreen` with a conversation context.

---

## Verification Plan

### Manual Verification
1.  **Conversation Persistence**: Start a support chat as a User, close the app, reopen, and verify the history is still there.
2.  **Real-time Admin Reply**: Send a message as a User; verify it appears instantly on the Admin Dashboard. Reply as Admin; verify it appears instantly in the app.
3.  **FCM Notification**: Close the app as a User. Send an Admin reply. Verify the User receives a Push Notification.
4.  **Auto-Reopen**: As Admin, "Close" a conversation. As User, send a new message. Verify the status flips back to "Open" in the dashboard.
