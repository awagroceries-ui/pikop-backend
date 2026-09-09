# Implementation Plan - Fix Onboarding Errors & KYC Sync

This plan fixes the "Duplicate Key" error in fulfiller activation, ensures KYC verification syncs with the admin dashboard, and polishes the Date Picker UI.

## Problem Description
1.  **Constraint Mismatch:** The `ON CONFLICT (user_id)` logic is failing because `user_id` lacks a unique constraint in the database.
2.  **Email Conflict:** Fulfillers encounter `fulfillers_email_key` violations, likely due to orphaned records from previous test attempts or deleted users.
3.  **KYC Dashboard Sync:** Verification results are saved in a secondary field (`didit_verification_status`) but aren't moving the main `kyc_status` forward, making them invisible to admins in the primary status column.
4.  **Date Picker UI:** The current implementation might be hard to trigger or still allowing manual input in some states.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725591000000_add_unique_user_id_to_fulfillers.js)
- Add a unique constraint to `fulfillers(user_id)`. This is required for the `UPSERT` (ON CONFLICT) logic to work.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **`updateFulfillerProfile`**:
    - Before the UPSERT, delete any existing fulfiller records that have the same `email` or `phone` but a **different** (or null) `user_id`. This cleans up "orphaned" records that cause duplicate key errors.

#### [MODIFY] [webhookController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/webhookController.js)
- **`handlePremblyWebhook`**:
    - When a verification is `approved`, also update the main `kyc_status` to `'PENDING_REVIEW'` (or a new state like `'ID_VERIFIED'`).
    - This ensures the record appears as "Progressed" on the Admin Dashboard immediately.

---

### Android App

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Date Picker Polish:**
    - Ensure the `OutlinedTextField` is fully disabled for manual input but wrapped in a reliable clickable `Box`.
    - Improve the `DatePickerDialog` logic to handle initial states more gracefully.
- **Refresh Logic:**
    - Increase the frequency of the profile refresh when a user returns to the app from the external verification browser.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Duplicate Test:** Try to activate a fulfiller multiple times. Verify no "Duplicate Key" or "Constraint Matching" errors occur.
2.  **Date Picker:** Open onboarding and verify the calendar appears immediately upon tapping the DOB field.
3.  **KYC Sync:** Complete a test verification. Verify the fulfiller status updates on the **Admin Dashboard** Mission Board and Fleet list without manual intervention.
