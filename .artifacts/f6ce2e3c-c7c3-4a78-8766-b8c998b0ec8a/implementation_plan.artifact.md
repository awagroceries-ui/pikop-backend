# Implementation Plan - Fix Fulfiller Status Sync & Navigation

This plan resolves the two reported issues: (1) post-verification navigation loop and (2) approved status not reflecting in the app.

## Problem Description
1.  **Stale Post-Verification State:** After finishing identity verification, the app refreshes the profile once. Due to webhook latency, the status might still be "pending" on the server. The app stays on the verification screen instead of advancing.
2.  **Dashboard Status Desync:** `MainActivity` and `KycViewModel` do not update `TokenManager` regularly. Even after an admin approves a fulfiller, the dashboard (which reads from `TokenManager`) shows the old "Unverified" state.
3.  **Local vs Global State:** `KycUploadScreen` and `FulfillerDashboardScreen` have isolated refresh logic that doesn't share updates through the global `TokenManager`.

## Proposed Changes

### Android App

#### [MODIFY] [KycViewModel.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycViewModel.kt)
- Update `refreshProfile()` to also sync the new `kyc_status` to `TokenManager`. This ensures that any update found while on the verification screen is immediately visible to the dashboard.

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Status Polling:** Add a `LaunchedEffect` that polls the profile every 5 seconds if the current step is `3` (Identity Verification) and the status is still `pending`. This handles webhook latency automatically.
- **Auto-Advance:** Ensure that if `kyc_verification_status` becomes `approved`, the screen automatically moves to the next logical step (Vehicle or Bank).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- **Background Polling:** Add a global polling loop (every 30-60 seconds) that refreshes the user profile if `userRole == "FULFILLER"` and `kycStatus != "VERIFIED"`. This ensures admin approvals are picked up without requiring an app restart or manual refresh.

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Ensure the "Verify Now" button is only shown if `kycStatus` is `NOT_STARTED` or `REJECTED`.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Post-Verification:** Complete an identity scan. Wait a few seconds on the app without clicking anything. Verify the screen automatically moves forward as the webhook completes.
2.  **Admin Approval:** Submit for review. On the dashboard, see "Verification Under Review". Approve the account as admin. Wait for the background sync (or manual refresh). Verify the dashboard switch unlocks and the warning disappears.
3.  **App Restart:** Close and reopen the app after approval. Verify the state is correctly persisted as `VERIFIED`.
