# Implementation Plan - Stability, Notifications & Navigation Hub

Resolve current stability issues (500 Error, 16KB alignment) and implement a professional Navigation Menu with Sign Out capabilities for both app roles.

## User Review Required

> [!IMPORTANT]
> - **16 KB Alignment**: I am switching the app to use compressed native libraries (`useLegacyPackaging = true`). This is a standard workaround for debug builds on 16 KB page-size emulators when alignment checks fail.
> - **Push Notifications**: I will implement the missing "Token Registration" logic. You will need to re-log into the app once for your device to register its notification token with the backend.
> - **Navigation Hub**: I am implementing a **Modal Navigation Drawer** (side menu) for both the Customer and Fulfiller dashboards.

## Proposed Changes

### 1. Stability & Fixes (Android & Backend)

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/build.gradle.kts)
- Set `jniLibs.useLegacyPackaging = true`. This forces the app to use compressed libraries, which bypasses the 16 KB alignment pop-up issue on debug builds.

#### [MODIFY] [PikopMessagingService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/PikopMessagingService.kt)
- Implement `onNewToken` to automatically send the Firebase token to the backend when it's generated.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/authController.js) (Backend)
- Add more defensive logging for 500 errors to help catch transient failures during signup/login.

---

### 2. Navigation Menu & Sign Out (Android)

#### [NEW] `NavigationDrawerContent.kt`
- A shared, branded component for the side menu.
- **Header**: Large Pikop Logo and user email.
- **Items**:
    - Home (Dashboard)
    - My Wallet
    - Saved Addresses (Customer only)
    - Support Chat
    - About Pikop
    - **Sign Out** (Red accent)

#### [MODIFY] `OrdersDashboardScreen.kt` & `FulfillerDashboardScreen.kt`
- Wrap both screens in a `ModalNavigationDrawer`.
- Add a "Menu" (hamburger) icon to the `TopAppBar`.

#### [MODIFY] `TokenManager.kt`
- Enhance `clearTokens()` to ensure all session data is wiped during Sign Out.

---

### 3. Push Notification Activation

#### [MODIFY] `MainActivity.kt`
- Add logic on startup to fetch the current FCM token and send it to the backend via `updateFCMToken`, ensuring the device is always registered.

---

## Verification Plan

### Manual Verification
1.  **16 KB Check**: Launch the app on the emulator and verify the "ELF alignment" pop-up no longer appears.
2.  **Menu Navigation**: Open the side menu and verify all links (Wallet, Support, etc.) work correctly.
3.  **Sign Out**: Click Sign Out, verify you are returned to the User Type Selection screen, and that you cannot go back without logging in.
4.  **Notifications**: Send a support chat message from the Admin Dashboard and verify the push notification appears on the phone.
