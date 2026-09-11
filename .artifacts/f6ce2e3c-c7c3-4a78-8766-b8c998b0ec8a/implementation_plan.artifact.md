# Implementation Plan - Fix Fulfiller Mission History

This plan addresses the issue where a fulfiller's mission history remains empty despite having accepted and completed real missions.

## Diagnostic Findings
1.  **Duplicate Logic**: Fulfiller history logic exists in both `fulfillerController.js` and `orderController.js`. The routes currently point to the "legacy" version in `fulfillerController.js`.
2.  **Missing Field**: The version in `fulfillerController.js` does not return the `earnings` field, which the Android app uses for its header and list items. This can lead to ₦0.00 displays or parsing errors if not handled gracefully.
3.  **Potential ID Mismatch**: The query relies on finding a record in the `fulfillers` table that matches the `user_id` from the authenticated session. If this link is missing or if the `fulfiller_id` in the `orders` table is incorrectly set, the history will be empty.
4.  **Status Handling**: The current query in `fulfillerController` is very basic and does not support the filtering (active/completed) that the app might expect or that was intended in the V3 upgrade.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Authoritative `getFulfillerOrders`**: Refactor this function to be the single source of truth for agent history.
- **Robust SQL**:
    - Select all relevant columns from `orders`.
    - Join `fulfillers` once to verify the agent's identity.
    - Calculate `earnings` (75% share) server-side for consistency.
    - Implement a case-insensitive status filter (`all`, `active`, `completed`).
- **Data Integrity**: Explicitly cast `user_id` to integer to prevent type mismatches.

#### [MODIFY] [fulfillerRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/fulfillerRoutes.js)
- Update the `/orders` route to use `orderController.getFulfillerOrders` instead of the legacy `fulfillerController` version.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- Remove the redundant and outdated `getFulfillerOrders` function to prevent future confusion.

### Android App
- No changes required. The app already calls `api/v1/fulfillers/orders`. By fixing the backend routing and query, the app will instantly show the correct history.

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Verify JSON response structure matches `FulfillerOrderResponse`.

### Manual Verification
1.  **Historical Data Trace**: Log in as a fulfiller with known completed missions. Verify history shows all missions (completed and cancelled).
2.  **Active Mission Trace**: Accept a new mission. Verify it appears in history immediately with the correct status.
3.  **Earnings Audit**: Verify the "Lifetime Earnings" header in the app matches the sum of the 75% shares returned by the new query.
4.  **Cancelled State**: Cancel a mission (as a customer) and verify it still appears in the agent's history as `CANCELLED`.
