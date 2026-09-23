# Implementation Plan - Admin KYC Approval & Fulfiller Active Mission Persistence

This plan resolves the missing `approved_at` column error during Admin KYC approval and fixes the active mission persistence and "RESUME" functionality on the Fulfiller Dashboard and Delivery History screens.

## Proposed Changes

### 1. Admin Verification Queue KYC Approval Schema Fix
#### [NEW] [1726900000000_ensure_approved_at_column.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726900000000_ensure_approved_at_column.js)
- Add migration to ensure `approved_at` timestamp column exists on `fulfillers` table:
  `ALTER TABLE "fulfillers" ADD COLUMN IF NOT EXISTS "approved_at" timestamp;`

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `updateKYCStatus` SQL query to set `approved_at = CURRENT_TIMESTAMP` when `status === 'VERIFIED'` without throwing missing column errors.

---

### 2. Fulfiller Active Mission Persistence & Resume Functionality
#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Expand active mission check in dashboard:
  ```kotlin
  val activeMission = history.firstOrNull {
      val s = it.status?.uppercase() ?: ""
      s.isNotBlank() && s !in listOf("DELIVERED", "CANCELLED", "RELEASED", "REFUNDED")
  }
  ```
- Ensure the **"ACTIVE MISSION IN PROGRESS 🚀"** banner displays prominently at the top of the dashboard for any active un-completed mission, allowing one-tap resumption into `active_order/$orderId`.

#### [MODIFY] [FulfillerOrdersScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerOrdersScreen.kt)
- Expand `canResume` logic on Delivery History order cards:
  ```kotlin
  val canResume = order.status?.uppercase() !in listOf("DELIVERED", "CANCELLED", "RELEASED", "REFUNDED")
  ```
- Enable the **"RESUME"** button for all in-progress orders so Fulfillers can return to active mission navigation from the Delivery History screen at any time.

---

## User Review Required

> [!IMPORTANT]
> **Admin KYC Approval**
> Approving a Fulfiller in the Admin Verification Queue will now record `approved_at = CURRENT_TIMESTAMP`, send the customized welcome email, and transition status to `VERIFIED` without errors.

> [!NOTE]
> **Deployment Requirement**
> Deploying these updates requires building the updated Android App Bundle (`app-release.aab`), installing the release APK on the test device, and running `git pull origin main && npm run migrate:up && pm2 restart pikop-v3` on your VPS server.

---

## Verification Plan

### Automated Tests
- Verify Node.js syntax for all modified controller files using `node -c`.
- Build release app bundle (`app-release.aab`) using Gradle.

### Manual Verification
1. **Admin Verification Queue**: Open `/admin/kyc` in Admin Dashboard, click "Approve" on a Fulfiller. Confirm approval succeeds and transitions Fulfiller to `VERIFIED`.
2. **Fulfiller Active Mission Persistence**: Accept a mission as a Fulfiller, navigate back to the main dashboard or leave the app, reopen Fulfiller Dashboard. Confirm the **"ACTIVE MISSION IN PROGRESS"** banner is visible and tapping **"RESUME"** returns directly to active mission navigation.
