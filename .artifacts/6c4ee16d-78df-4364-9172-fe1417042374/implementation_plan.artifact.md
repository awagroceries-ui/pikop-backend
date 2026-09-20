# Implementation Plan - System Hardening & Bug Fixes

This plan addresses critical race conditions, idempotency gaps, and security risks identified during the comprehensive audit.

## Proposed Changes

### 1. Backend: Financial & State Hardening

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **Fix Duplicate Creation**: Move the "Order already exists" check *inside* the database transaction for both regular and commerce orders.
- **Unique References**: Ensure `payment_reference` has a unique constraint at the DB level (if not already present) to prevent duplicate inserts even under extreme race conditions.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Fix Acceptance Race Condition**: In `acceptOrder`, lock the fulfiller's row (`SELECT FOR UPDATE` on `fulfillers`) before checking if they have active orders. This prevents a fulfiller from being assigned two missions simultaneously.

#### [MODIFY] [scheduledOrderJob.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/jobs/scheduledOrderJob.js)
- **Batch Processing**: Add a `LIMIT 50` to the scheduled order update to prevent massive spikes from overwhelming the dispatcher.

---

### 2. Backend: Security & Stability

#### [MODIFY] [marketplaceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/marketplaceController.js) & [kitchenController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/kitchenController.js)
- **Restrict Data Exposure**: Update `getVendorDetails` and `getKitchenDetails` to only return necessary public fields (business name, city, category, description, profile photo) rather than `SELECT *`.

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- **Safe JSON Parsing**: Wrap `JSON.parse(item.operating_hours)` in a try-catch block within the `getDiscovery` mapping logic to prevent a 500 error if operating hours data is malformed.

---

### 3. Android App: Reliability & Error Handling

#### [MODIFY] [SupportHubScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SupportHubScreen.kt)
- **Error Feedback**: Update `fetchArticles` to show an error message and a "Retry" button if the API call fails, instead of just remaining empty.

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- **State Guarding**: Ensure that sensitive buttons (like "Verify Delivery") are disabled while an API call is in progress to prevent duplicate status updates from the app side.

---

## User Review Required

> [!CAUTION]
> **Database Locking**
> Using `SELECT FOR UPDATE` on fulfillers during order acceptance will serialize assignment for that specific agent. This is necessary for data integrity but means if an agent's connection is extremely laggy, it might briefly lock their record. This is acceptable for the benefit of preventing double-assignment.

## Verification Plan

### Automated Tests (Scripts)
- **Concurrency Test**: Run 5 simultaneous "accept" requests for the same fulfiller on different orders. Confirm only 1 is assigned as primary and others are queued correctly.
- **Webhook Replay**: Trigger the same Paystack `charge.success` webhook twice for a commerce order. Confirm only 1 order is created in the database.

### Manual Verification
- **App Resilience**: Temporarily disconnect the internet while loading the Help Center. Confirm the "Retry" button appears and correctly re-fetches content when reconnected.
