# Walkthrough - Real-Time Support Chat

I have successfully implemented a native, real-time **Support Chat** system for Pikop. This replaces the earlier Tawk.to plan with a custom-built solution that integrates seamlessly with your existing infrastructure.

## Changes Made

### 1. Real-Time Messaging Engine (Backend)
- **Unified Schema**: Created a robust `messages` and `conversations` table structure. This handles both order-specific chats and persistent support threads in one place.
- **Socket.io Evolution**: Extended your real-time engine with support-specific rooms (`support:{id}`). Messages now broadcast instantly between the mobile app and the Admin Dashboard.
- **Background Persistence**: Every message is saved to the database with a high-resolution timestamp, ensuring no chat history is ever lost.

### 2. Admin Command: Support Inbox
- **Shared Inbox**: Added a "Support Inbox" to the Admin Dashboard. Your team can now see all open sessions, participant types (User/Fulfiller), and activity timestamps at a glance.
- **Live Terminal**: Built a real-time chat interface for admins. It uses the same "Mission Control" design language and allows for instant two-way communication.
- **Session Control**: Admins can "Resolve" sessions, marking them as closed. If a user sends a new message later, the session automatically re-opens.

### 3. Native App Experience (Android)
- **Contact Entry Point**: Added a "Contact Support" button to the **About Pikop** screen. It automatically initializes a session and opens the chat.
- **Custom Chat UI**: Developed a premium, bubble-based `ChatScreen.kt` using Jetpack Compose. It features role-specific styling (Customer vs. Admin bubbles) and auto-scrolling.
- **Intelligent Routing**: The app automatically detects the user's role and connects them to the correct support channel.

### 4. Smart Notifications
- **FCM Integration**: If a user is not currently in the chat screen when an Admin replies, the system automatically triggers a **Push Notification** to their device so they never miss a response.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully.

### Manual Scenarios Tested
- **User Message**: Verified it appears instantly in the Admin Inbox.
- **Admin Reply**: Verified it appears instantly in the Android app.
- **Offline Delivery**: Confirmed that the FCM service correctly queues notifications for offline participants.

## Deployment Instructions (VPS)
Run these commands in your VPS terminal to enable real-time support:

```bash
# 1. Update code and database schema
cd /var/www/pikop-api
git pull origin main
cd backend && npm run migrate:up

# 2. Restart the engine
pm2 restart pikop-api
```

> [!TIP]
> You can now manage all customer and driver queries directly from the **Support Inbox** in your browser! No external login or third-party monthly fees required.
