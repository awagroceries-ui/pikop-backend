# Implementation Plan - Production Fixes & Real-Road Map Routing

This plan addresses 8 critical production bugs, UI contrast/sizing issues, email validation safeguards, Admin Panel account deletion capabilities, and Real-Road Network Navigation Polylines.

## Proposed Changes

### 1. Fulfiller Mission Acceptance Fix
#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Expand Status Check**: Update `acceptOrder` allowed status check from `['SEARCHING', 'PAYMENT_CAPTURED']` to `['SEARCHING', 'PAYMENT_CAPTURED', 'PAID', 'PROCESSING', 'CONFIRMED', 'SCHEDULED', 'QUEUED']`.
- **Idempotent Claim**: If an order is already assigned to the requesting fulfiller (`rows[0].fulfiller_id === fulfillerId`), return `200 OK` with status `'MATCHED'` instead of rejecting with `"Order is no longer available"`.

---

### 2. Role Selector Icon Visibility in Dark Mode
#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- **Color Contrast**: Replace `PikopNearBlack` (`#111827`) icon color for "Business Account" with `MaterialTheme.colorScheme.primary` / `PikopGreen` to guarantee 100% visibility in both Light Mode and Dark Mode.

---

### 3. Role Selector Layout & Icon Sizing (Zero-Scroll UI)
#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- **Compact Layout**:
  - Reduce logo size from `180.dp` to `80.dp`.
  - Reduce vertical spacers from `48.dp` / `32.dp` to `12.dp` / `16.dp`.
  - Reduce `RoleCard` height from `180.dp` to `105.dp`.
  - Reduce `Icon` size inside `RoleCard` from `56.dp` to `32.dp`.
  - Ensure all 5 role options + "Already have an account? Log In" link fit on screen without scrolling.

---

### 4. In-App Account Deletion Fix
#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **Subquery Guard**: Update `DELETE FROM kyc_documents` query to use `IN`:
  `DELETE FROM kyc_documents WHERE fulfiller_id IN (SELECT id FROM fulfillers WHERE user_id = $1)`
- **Unique Anonymization**: Append timestamps to anonymized email and phone strings (`deleted_15_1790158000@pikop.ng` / `del_15_17901580`) to prevent database uniqueness collisions.
- **Detailed Error Reporting**: Return `error.message` in catch block responses to prevent silent failures.

---

### 5. Admin Panel User Account Deletion Capability
#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add POST `/admin/users/:id/force-delete` route.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement `forceDeleteUser` handler to purge unverified/malformed user accounts and clean up associated records.

#### [MODIFY] [customer_detail.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/customer_detail.ejs) & [fulfiller_detail.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fulfiller_detail.ejs)
- Add a red **"Delete User Account"** action button in Admin Customer and Fulfiller detail views.

---

### 6. Strict Email Validation on Signup
#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Add email regex validation in `signup()`: `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`. Reject any request where `email` is not a valid email address (e.g. physical home addresses).

#### [MODIFY] Android Signup Screens
- Update `SignupCustomerScreen.kt`, `SignupFulfillerScreen.kt`, `SignupMerchantScreen.kt`, `SignupCorporateScreen.kt`, and `SignupFleetPartnerScreen.kt` to validate email format before enabling the Signup button.

---

### 7. Demand Hotspot Location Resolution
#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Update `cameraPositionState` when `profile` or `hotspots` load to center on the fulfiller's actual coordinates/state instead of hardcoding to Lagos (`6.5244, 3.3792`).

---

### 8. Real-Road Network Map Navigation & Route Polylines
#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt) & [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- **Replace Dotted Lines**: Replace the straight dashed `Polyline` with real land road navigation polylines.
- **Directions API / Polyline Decoding**: Fetch road network route waypoints using Google Directions API or encoded polyline decoder, drawing smooth, solid road-following polylines along real streets, avenues, and highways.
- **Smooth Road Navigation**: Animate the Fulfiller marker smoothly along actual road polyline waypoints as location updates occur.

---

## User Review Required

> [!IMPORTANT]
> **Real-Road Navigation**
> Live tracking maps on both Customer and Agent screens will now draw actual road network navigation paths following real streets, turns, and highways instead of straight dotted lines.

> [!NOTE]
> **Deployment Requirement**
> Applying these updates will require deploying the updated Android App Bundle (`app-release.aab`) and running `git pull origin main && pm2 restart pikop-v3` on your VPS server.

---

## Verification Plan

### Automated Tests
- Verify Node.js syntax for all modified controller files using `node -c`.
- Build release app bundle (`app-release.aab`) using Gradle.

### Manual Verification
1. **Fulfiller Claiming**: Test accepting a mission as a fulfiller; verify status updates to `MATCHED` and appears in active mission history.
2. **Role Selector**:
   - Check Dark Mode visibility for "Business Account" icon.
   - Verify all 5 cards and the Login link fit on screen without scrolling.
3. **Account Deletion**:
   - Test in-app account deletion from Settings > Account.
   - Test Admin forced account deletion from `/admin/customers/:id`.
4. **Email Validation**: Try signing up with a home address in the email field. Verify it is blocked with a validation error.
5. **Demand Hotspots**: Toggle Demand Hotspots on Fulfiller Dashboard. Confirm the map centers on the fulfiller's actual location.
6. **Real-Road Map Routes**: View live map tracking. Verify the polyline follows real road networks and streets instead of a straight dashed line.
