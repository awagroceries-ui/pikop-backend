# Implementation Plan - Crash Fix & UI Refinement

Resolve the instant app crash, fix the persistent 16 KB alignment warning, and improve UI visibility across the platform.

## User Review Required

> [!IMPORTANT]
> - **Firebase Initialization**: I suspect the crash is due to Firebase Messaging being called before it's properly initialized (as `google-services.json` appears to be missing). I will wrap these calls in safety checks.
> - **16 KB Warning**: I will try a different combination of build flags to permanently silence this development warning on the emulator.
> - **UI Contrast**: I will continue brightening text and labels to ensure they are visible on all screens.

## Proposed Changes

### 1. Crash Fix (Android)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Wrap `FirebaseMessaging.getInstance()` calls in a `try-catch` block and check if `FirebaseApp` is initialized.
- Ensure `PikopAppNavigation` doesn't crash if the intent handling fails.

#### [MODIFY] [PikopApp.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/PikopApp.kt)
- Add a safety check for Firebase initialization.

---

### 2. Build & Compatibility (Android)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/AndroidManifest.xml)
- Remove `extractNativeLibs` and `pageSizeCompat` if they continue to cause conflicts, relying purely on Gradle's `useLegacyPackaging`.
- Or, use the specific combination recommended for Android 15 testing.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/build.gradle.kts)
- Ensure `useLegacyPackaging = true` is correctly set in the `packaging` block.

---

### 3. UI Contrast & Login Fix (Full Stack)

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Standardize all text colors to use `MaterialTheme.colorScheme.onSurface` for high contrast.

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/layout.ejs)
- Apply a even brighter color to the sidebar links and main data points.

---

## Verification Plan

### Manual Verification
1.  **Run the App**: Confirm it no longer crashes instantly upon launch.
2.  **Alignment Pop-up**: Verify if the 16 KB warning is suppressed.
3.  **Login Test**: Verify that valid credentials work (note: if you wiped the DB, you must sign up again).
4.  **Visual Audit**: Check the "Request a Delivery" and "Admin" screens for text visibility.
