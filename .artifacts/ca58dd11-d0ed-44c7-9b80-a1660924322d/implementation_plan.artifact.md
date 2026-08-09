# Implementation Plan - Final Stability & Notification Routing

Resolve the persisting 16 KB alignment warning, the 500 Server Error during signup, and implement deep-link routing for push notifications.

## User Review Required

> [!IMPORTANT]
> - **16 KB Alignment**: I am explicitly enabling `extractNativeLibs="true"` in the Manifest. This ensures libraries are compressed, which is the standard way to bypass alignment warnings on debug emulators while maintaining full functionality.
> - **Notification Routing**: I will update the `MainActivity` to listen for "New Message" notifications and automatically open the support chat when the notification is tapped.
> - **500 Error Diagnostic**: I have added a specialized signup diagnostic to the backend to catch the exact silent error causing the signup failure.

## Proposed Changes

### 1. Build & Compatibility (Android)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/AndroidManifest.xml)
- Add `android:extractNativeLibs="true"` to the `<application>` tag.
- Add `android:pageSizeCompat="enabled"`.
- This combination is the recommended "debug-mode" fix to suppress the 16 KB alignment pop-up.

---

### 2. Notification Deep-Linking (Android)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add logic to check the `Intent` for a `navigate_to` extra.
- If `chat` is detected, automatically navigate the user to the support conversation after they log in.

---

### 3. Backend Hardening (Backend)

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/authController.js)
- Add a specialized check for the `wallets` table during signup.
- Enhance the `ROLLBACK` logic to ensure the database connection is never left hanging.

#### [NEW] `backend/scratch/fix_database.js`
- A script to run on your VPS that ensures the `role` column and `wallets` entries are perfectly formatted, fixing any silent data corruption from previous failed tests.

---

## Verification Plan

### Manual Verification
1.  **Alignment Check**: Launch the app and verify the 16 KB pop-up is finally gone.
2.  **Signup Success**: Run the `fix_database.js` on your VPS, then attempt a signup. You should receive the OTP and proceed to the dashboard.
3.  **Notification Tap**: Send a test message from the Admin Dashboard, tap the notification on the phone, and verify it opens the Chat screen.
4.  **Logout**: Navigate to the "Account" tab in the bottom bar and verify the "Sign Out" button works and clears your session.
