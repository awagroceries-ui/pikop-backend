# Implementation Plan - Fix Fulfiller Activation & Webhook Sync

This plan addresses the "Duplicate Key" error during account activation, fixes the Prembly KYC webhook synchronization, and implements a native Date Picker for the onboarding flow.

## Problem Description
1.  **Duplicate Key Error:** Fulfillers encounter a database error during activation because the system tries to insert a new profile when one already exists for their email.
2.  **KYC Loop:** After successful verification on Prembly, the app doesn't advance. This is due to a "Payload Too Large" error blocking the webhook on the VPS (which I have already addressed in a previous step, but requires verification) and potential mapping issues in the webhook handler.
3.  **Manual Date Entry:** The DOB field requires manual typing, which is prone to errors.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **`updateFulfillerProfile`**: Refactor the profile update logic to use a single `UPSERT` query (`INSERT ... ON CONFLICT (user_id) DO UPDATE`). This entirely eliminates the risk of "Duplicate Key" errors.

#### [MODIFY] [webhookController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/webhookController.js)
- **Robust Webhook:**
    - Improve logging for the Prembly webhook to capture the raw payload for debugging.
    - Ensure `userId` is parsed correctly as an integer.
    - Update the query to be idempotent.

---

### Android App

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Native Date Picker:** Replace the manual DOB text field with a clickable field that launches a Material 3 `DatePickerDialog`.
- **Advancement Logic:**
    - Improve the `LaunchedEffect` that observes the profile state.
    - Ensure the app explicitly checks for `didit_verification_status == 'approved'` (which maps to the Prembly result).
- **Status Polling:** Add a temporary polling mechanism or a more prominent "Refresh Verification Status" button to ensure the user isn't stuck if the socket message is missed.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **Activation Fix:** Attempt to update the fulfiller profile twice. Verify no "Duplicate Key" error occurs.
2.  **Date Picker:** Open the "Personal Details" step and select a date using the calendar UI.
3.  **KYC Webhook Test:**
    *   Simulate a Prembly webhook locally (if possible) or trigger a test verification.
    *   Verify the `fulfillers` table is updated correctly with `approved` status.
    *   Verify the app automatically advances to the next step (Vehicle/Bank).
