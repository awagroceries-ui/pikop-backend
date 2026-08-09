# Implementation Plan - Final Stability & UI Refinement

Finalize the app's stability, activate push notifications globally, and implement a professional Bottom Navigation Bar for intuitive access to core features.

## User Review Required

> [!IMPORTANT]
> - **Build Fix**: I have already resolved the "extractNativeLibs" build failure. You can now build and run the app successfully.
> - **UI Structure**: I will implement a **Bottom Navigation Bar** (Home, Orders, Wallet, Menu) to replace the current TopAppBar navigation, making the app feel more like a premium marketplace.
> - **Push Notifications**: I will ensure the FCM token registration is robust and tested.

## Proposed Changes

### 1. UI Architecture (Android)

#### [MODIFY] `MainActivity.kt` & Dashboards
- Implement a **BottomNavigationBar** component.
- **Tabs**:
    - **Home**: Main Dashboard (Create Order / Receive Offers).
    - **Orders**: Mission History.
    - **Wallet**: Earnings and Payouts.
    - **Account**: Profile, Support, and **Sign Out**.

---

### 2. Push Notification Mastery

#### [MODIFY] `PikopMessagingService.kt`
- Add support for **In-App Chat** notifications (Support and Order chats).
- Ensure notifications include a deep-link to the specific chat or order.

---

### 3. Backend Reliability

#### [MODIFY] `diditService.js` (Backend)
- Add a fallback mechanism to use a placeholder session if the API key is missing (for development only), ensuring the UI doesn't crash during testing.

#### [MODIFY] `authController.js`
- Standardize all 500 error responses to return a JSON object with a `detail` field, matching the Android app's error parsing.

---

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to confirm build success.

### Manual Verification
1.  **Navigation**: Tap through the new Bottom Navigation tabs and verify they switch screens instantly.
2.  **Logout**: Navigate to the "Account" tab and click "Sign Out." Verify the session is wiped.
3.  **Stability**: Trigger a signup with an existing email and verify you get a clean "Email already exists" message instead of a 502/500 error.
4.  **Notifications**: Send a chat message and verify the push notification appears with the correct icon and text.
