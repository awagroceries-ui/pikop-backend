# Implementation Plan - Fix Cross-State Order Dispatch (20km Radius)

This plan resolves the critical bug where orders are dispatched across state boundaries and implements smooth live tracking with a strictly enforced 20km radius.

## Problem Description
1.  **Dispatch Leakage:** The `getAvailableOffers` API lacks distance or state filtering, allowing any online agent to see every mission in the database.
2.  **Missing Regional Data:** Structured "state" data is not stored in the `orders` or `fulfillers` tables, making it impossible to perform hard regional filtering.
3.  **Stale Locations:** Fulfillers' locations are not updated when they are "idle" but online, leading to incorrect distance calculations.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725588000000_add_state_filtering_columns.js)
- Add `pickup_state` to `quotes` and `orders` tables.
- Add `current_state` to the `fulfillers` table.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getQuote` & `createOrder`**: Accept `pickup_state` from the request and persist it.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **`updateStatus`**: Accept and save `current_state`.
- **`getAvailableOffers`**:
    - **Hard Filter:** Add `AND o.pickup_state = f.current_state` to the SQL query.
    - **Radius Filter:** Add `AND ST_DWithin(f.current_location::geography, o.pickup_location::geography, 20000)` (Strict 20km limit).
    - **Staleness Guard:** Filter out fulfillers whose `last_ping_at` is older than 30 minutes.

#### [MODIFY] [dispatchService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/dispatchService.js)
- Update `findNearbyFulfillers` to include the `current_state = order.pickup_state` hard filter and the 20km radius limit.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `QuoteRequest`, `CreateOrderRequest`, and `FulfillerStatusRequest` to include state fields.
- Update `OrderDetailsResponse` to include `pickup_state`.

#### [MODIFY] [MapAddressSearchScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/MapAddressSearchScreen.kt)
- Update the selection logic to resolve the `state` name from Google Places `ADDRESS_COMPONENTS`.
- Pass the state back to the caller via `onAddressSelected(address, lat, lng, state)`.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Capture and send the `pickup_state` during quote and order creation.

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- **Periodic Ping:** Implement a `LaunchedEffect` that pings the server with current coordinates and state every 60 seconds while `isOnline` is true to prevent staleness.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **State Separation:** Create an order in Port Harcourt. Verify a fulfiller in Lagos (with updated location) **cannot** see the offer in their list.
2.  **Radius Test:** Move a fulfiller to 21km from the pickup point. Verify the offer disappears from their list.
3.  **Staleness Test:** Disable location pings for a fulfiller for 31 minutes. Verify they no longer receive new offers until they ping again.
