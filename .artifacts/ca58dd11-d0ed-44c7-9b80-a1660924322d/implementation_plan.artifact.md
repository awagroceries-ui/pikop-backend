# Implementation Plan - Reliability & UI Contrast Fixes

Resolve the signup transaction bug, fix login authentication issues, and improve UI contrast for both the Admin Dashboard and the Android Order screens.

## User Review Required

> [!IMPORTANT]
> - **Signup Fix**: I found a critical bug where the signup process was failing because it tried to save your verification code before finishing the account creation. This will be fixed by properly grouping everything into a single secure transaction.
> - **UI Contrast**: I am increasing the brightness of all text in the Admin Portal and ensuring the Order screen in the app uses high-contrast text for maximum readability.

## Proposed Changes

### 1. Backend Reliability (Signup & Login)

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/authController.js)
- **Fix Signup Transaction**: Change all database calls within the signup process to use the same transaction client. This resolves the "foreign key constraint" violation.
- **Login Debugging**: Ensure the login process correctly identifies the user's role and verification status.

---

### 2. Admin Portal UI (Contrast Fix)

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/layout.ejs)
- Brighten `--text-muted` from `#94A3B8` to `#CBD5E1`.
- Add explicit high-contrast overrides for Bootstrap tables and card texts to ensure they are crisp against the Midnight Navy background.

---

### 3. Android UI (Contrast & 16 KB Fix)

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Wrap key text elements in `CompositionLocalProvider` or explicitly set their color to `MaterialTheme.colorScheme.onBackground` to ensure visibility against any background color.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/AndroidManifest.xml)
- Add `tools:ignore="UnusedAttribute"` to the `extractNativeLibs` and `pageSizeCompat` attributes to ensure they are correctly processed by the build tool and finally silence the 16 KB pop-up.

---

## Verification Plan

### Manual Verification
1.  **Signup**: Perform a signup and verify the "verification code" email is sent and no database error occurs.
2.  **Login**: Verify you can log in with the account created above.
3.  **Admin Contrast**: Open the Admin Dashboard and verify all sidebar links and table data are clearly readable.
4.  **Order Contrast**: Open the "Request a Delivery" screen and verify all labels and field titles are dark/bold enough to be seen easily.
5.  **Alignment Check**: Verify the 16 KB warning no longer appears on launch.
