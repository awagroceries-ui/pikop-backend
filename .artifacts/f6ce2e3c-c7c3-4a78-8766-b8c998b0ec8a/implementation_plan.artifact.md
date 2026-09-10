# Implementation Plan - Fix Fulfiller Dashboard Verification Status

This plan fixes the issue where fulfillers see an outdated "Account Not Verified" status on their dashboard even after completing the verification process.

## Problem Description
1.  **State Desync:** The `FulfillerDashboardScreen` has its own local `kycStatus` variable initialized to `"PENDING"`, which is never updated from the global `TokenManager` or the backend.
2.  **Missing Wiring:** `MainActivity` observes the correct `kycStatus` from `TokenManager` but fails to pass it as a parameter to `FulfillerDashboardScreen`.
3.  **Confusing UX:** The dashboard shows a generic "Account Not Verified" error even when the account is actually `PENDING_REVIEW`, which is frustrating for users who have finished their steps.

## Proposed Changes

### Android App

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Update the function signature to accept `kycStatus: String`.
- Remove the local `var kycStatus by remember { mutableStateOf("PENDING") }`.
- Refactor the **KYC Status Card** to distinguish between `NOT_STARTED` and `PENDING_REVIEW`:
    - If `PENDING_REVIEW`: Show a yellow "Verification Under Review" card with a message explaining the 24-hour SLA.
    - If `VERIFIED`: Hide the card entirely (as it already does).
- Ensure the **Online/Offline Switch** and **Bottom Message** are correctly enabled based on the passed-in `kycStatus`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Pass the `kycStatus` (collected from `TokenManager`) into the `FulfillerDashboardScreen` call.

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Pending State:** Log in as a fulfiller who has submitted for review. Verify the dashboard shows "Verification Under Review" instead of "Account Not Verified".
2.  **Verified State:** As an admin, approve a fulfiller. Verify the dashboard automatically updates (via background sync) to remove the warning and enables the "Online" switch.
3.  **Go Online:** Confirm that once `VERIFIED`, the agent can actually toggle the switch to "ONLINE".
