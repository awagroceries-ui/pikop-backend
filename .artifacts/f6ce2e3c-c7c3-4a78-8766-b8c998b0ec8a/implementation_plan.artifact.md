# Implementation Plan - Fix Fulfiller History, Customer Rating & Payment Options

This plan addresses the missing fulfiller history, adds customer-to-fulfiller ratings, and provides a final attempt at forcing Bank Transfer in Paystack.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getFulfillerOrders`**: Add logging to debug why the history appears empty for some fulfillers.
- **`rateFulfiller` [NEW]**: Implement an endpoint to allow customers to rate their fulfillers.
- **`verifyDelivery`**: Double-check the 500 error persistence. Ensure `parseFloat` is used for all item price logic.

#### [MODIFY] [orderRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/orderRoutes.js)
- Register `POST /:orderId/rate-fulfiller`.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Experiment with `channels` order: `['card', 'ussd', 'bank', 'bank_transfer', 'qr', 'mobile_money']`.
- Some Paystack accounts require `bank` and `bank_transfer` to be listed together.

---

### Android App

#### [MODIFY] [FulfillerOrdersScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerOrdersScreen.kt)
- Add a **"RESUME MISSION"** button to the `FulfillerOrderCard`.
- This button will appear for missions with statuses like `MATCHED` or `PICKED_UP`, allowing fulfillers to return to a "stuck" mission.

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Add a `RatingDialog` specifically for the customer to rate the fulfiller.
- The dialog should appear automatically (or via a button) once the mission status is `DELIVERED` or `RELEASED`.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `rateFulfiller(orderId: String, request: RatingRequest)` to the interface.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1. **Fulfiller History:** Fulfiller should see their completed missions. If empty, check VPS logs for `[FulfillerOrders] Query for ID: ... yielded X results`.
2. **Resume Mission:** Fulfiller should be able to click "RESUME" on an active mission from their history tab.
3. **Rating:** Customer should see a rating option after confirming receipt of their item.
4. **Paystack:** Verify if "Bank Transfer" finally appears.
