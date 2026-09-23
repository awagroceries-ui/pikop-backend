# Implementation Plan - Production Bug Fixes, Fulfiller Claiming & Policy Routes

This plan addresses the "Cannot GET /terms/fulfiller" policy error, the Fulfiller order acceptance response structure mismatch, and the complete set of UI/UX, email validation, and admin account deletion features.

## Proposed Changes

### 1. Fulfiller Conduct Policy Web Route & Android Viewer
#### [MODIFY] [legalRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/legalRoutes.js)
- Add GET `/terms/fulfiller` and `/terms-fulfiller` routes.

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- Add `getFulfillerTerms` handler rendering Fulfiller Terms & Conduct Policy.

#### [MODIFY] [app.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/app.js)
- Mount `app.use('/terms', require('./routes/legalRoutes'))` alongside `/legal`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Update `terms_viewer/{showFulfillerTerms}` route to open `https://api.pikop.com.ng/legal/terms/fulfiller` when `showFulfillerTerms == true`.

---

### 2. Fulfiller Mission Acceptance & Active Mission Resume Banner
#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- In `acceptOrder`, return `status` at the top level of the JSON response in addition to `data.status`:
  `res.status(200).json({ success: true, status: resStatus, message: 'Mission Accepted', data: { status: resStatus } });`

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `OrderResponse` data class to include `data: OrderResponseData? = null`.

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Update `onAccept` handler to inspect `response.status ?: response.data?.status` and navigate to `active_order/$orderId` upon claim.
- Add an **"Active Mission in Progress"** banner at the top of the dashboard if the Fulfiller has an active claimed mission, allowing one-tap resumption.

---

### 3. Business Account Role Selector Icon Dark Mode Contrast
#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- Replace near-black icon color with Dodger Blue (`#2196F3`), ensuring 100% visibility in Dark Mode.

---

### 4. Role Selector Compact Zero-Scroll Layout
#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- Reduce logo size to `70dp`, card height to `100dp`, and icon size to `30dp`. All 5 role options + Login link fit on screen without scrolling.

---

### 5. In-App Account Deletion Fix
#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Fix subquery in `deleteAccount` and append unique timestamps to anonymized email and phone values.

---

### 6. Admin Panel User Account Deletion Capability
#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js) & [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Add POST `/admin/users/:id/force-delete` route and handler. Add "Delete User Account" button to Admin Customer and Fulfiller detail views.

---

### 7. Strict Email Validation on Signup
#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js) & Signup Screens
- Validate email format with regex on backend and across all 5 Android signup screens.

---

### 8. Demand Hotspot Location Resolution
#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Resolve map camera to Fulfiller's actual GPS location/state instead of hardcoding to Lagos.

---

### 9. Real-Road Network Map Navigation
#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt) & [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Render multi-segment road navigation polylines along real streets and highways.

---

## User Review Required

> [!IMPORTANT]
> **Fulfiller Order Acceptance Fix**
> Fulfillers tapping "ACCEPT MISSION" will now instantly see the status update to `MATCHED` and navigate directly into the active mission tracking screen.

> [!NOTE]
> **Deployment Requirement**
> Deploying these updates requires building the updated Android App Bundle (`app-release.aab`), installing the release APK on the test device, and executing `git pull origin main && pm2 restart pikop-v3` on your VPS server.

---

## Verification Plan

### Automated Tests
- Verify Node.js syntax for all modified controller files using `node -c`.
- Build release app bundle (`app-release.aab`) using Gradle.

### Manual Verification
1. **Terms Link**: Tap "Fulfiller Terms" in app. Confirm policy loads without "Cannot GET" error.
2. **Fulfiller Claiming**: Tap "ACCEPT MISSION" as a Fulfiller. Confirm app transitions immediately to active mission tracking and displays active mission banner.
3. **Role Selector**: Check Business Account icon in Dark Mode; verify zero-scroll layout.
4. **Email Check**: Try signing up with a home address in the email field; verify validation error.
5. **Real-Road Navigation**: View live map tracking; verify polyline follows street road networks.
