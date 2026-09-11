# Implementation Plan - Fix Fulfiller Mission Acceptance & Back Navigation

This plan addresses the issue where fulfillers "lose" an accepted mission if they press the back button, by ensuring missions are correctly registered on the server before navigation and providing a persistent way to resume them.

## Problem Description
1.  **Missing Server-Side Acceptance:** The Android app currently navigates to the "Active Mission" screen immediately when "Accept Mission" is clicked, but it **never actually calls the `acceptOrder` API**.
2.  **State Desync:** Because the mission is never officially assigned to the fulfiller on the server, it doesn't appear in their "Active Missions" history. When they press "Back," they return to a dashboard that doesn't show the mission they thought they accepted.
3.  **Navigation Ambiguity:** The `ActiveOrderScreen` lacks a "Back" button in the UI, making the device's back button feel like a "Cancel" action.

## Proposed Changes

### Android App

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Create a `handleAcceptMission(orderId)` function.
- This function will:
    - Set `isLoading = true`.
    - Call `apiService.acceptOrder(orderId)`.
    - On success: Navigate to `active_order/$orderId`.
    - On failure: Show a Toast (e.g., "Mission already taken by another agent").
- Update the `IncomingOfferComponent` callback to use this new handler.

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Add a `Scaffold` with a `TopAppBar`.
- Include a "Back" arrow that calls a new `onBack` callback (passed from `MainActivity`).
- This ensures fulfillers feel safe navigating away from the screen knowing the mission persists.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Pass `onBack = { navController.popBackStack() }` to `ActiveOrderScreen`.

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Acceptance:** Click "Accept Mission" on an offer. Verify the app shows a loader briefly and then navigates.
2.  **Back Navigation:** On the active mission screen, press the UI back button or device back button. Verify you return to the dashboard.
3.  **Resume:** Verify the primary blue "Resume Active Mission" banner appears on the dashboard immediately. Click it to return to the mission.
4.  **Race Condition:** Try to accept a mission that was already accepted by another fulfiller (simulated by admin or another device). Verify the app shows "Mission no longer available" instead of navigating.
