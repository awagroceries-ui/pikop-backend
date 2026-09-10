# Implementation Plan - Fix Fulfiller Visibility & Toggle Stability

This plan addresses the issues where fulfillers are not seeing active mission requests and the online status toggle is unstable.

## Problem Description
1.  **Mission Visibility Gap:** The polling endpoint for fulfiller offers (`getAvailableOffers`) uses strict string matching for the state (e.g., "Lagos" != "Lagos State"), filtering out missions that should be visible.
2.  **Toggle Desync:** The app dashboard initializes the "Online" state to `false` by default and does not fetch the actual status from the server on load. This makes the UI show "Offline" even if the server considers the agent "Online."
3.  **Fragile Lifecycle:** The background ping loop in the app stops if the local state is out of sync, leading to the agent "going offline" automatically from the server's perspective.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **Resilient Matching:** Update `getAvailableOffers` to use case-insensitive `ILIKE` and partial matching for the `current_state` filter, consistent with the `dispatchService.js`.
- **Match Alignment:** Ensure the query perfectly matches the active filtering logic used in the real-time dispatch service.

---

### Android App

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- **State Initialization:** Update the initial `LaunchedEffect` to fetch the fulfiller's profile and initialize the `isOnline` state from the server's `online_status`.
- **Toggle Hardening:** Improve error handling in the `Switch` toggle to ensure that if a server update fails, the UI correctly resets to the previous valid state.
- **Improved Loading Logic:** Ensure `isLoading` only affects the toggle during active transitions to prevent the "unclickable" state.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Mission Visibility:** Create an order in "Lagos State." Log in as a fulfiller in "Lagos." Verify the mission appears correctly on the dashboard.
2.  **Toggle Persistence:** Set fulfiller to "Online" and close the app. Reopen the app and verify the toggle correctly shows "Online" immediately after the profile sync.
3.  **Background Stability:** Keep the app open for 5 minutes and verify the fulfiller remains "ONLINE" on the server (check `last_ping_at` in DB or Admin panel).
