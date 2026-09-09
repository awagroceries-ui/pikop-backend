# Implementation Plan - Fix Fulfiller Rating Submission Error

This plan addresses the error encountered when customers attempt to rate their fulfillers after a mission, and completes the rating system by adding support for fulfillers to rate customers.

## User Review Required

> [!IMPORTANT]
> I have identified that while `rateFulfiller` (customer rating agent) was partially implemented on the backend, the corresponding `rateCustomer` (agent rating customer) endpoint was missing entirely. Additionally, the error handling on the mobile app was swallowing specific server-side error messages, making it difficult to diagnose the "Process Failure."

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725586000000_add_fulfiller_rating_to_orders.js)
- Add `fulfiller_rating` (integer) and `fulfiller_comment` (text) columns to the `orders` table. This allows agents to rate customers.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`rateFulfiller`**:
    - Add a check for existing ratings: return `400` with message "You've already rated this mission" if `customer_rating` is not null.
    - Add status check: Ensure mission is `DELIVERED`, `RELEASED`, or `COMPLETED`.
    - Use `COALESCE` in the average rating calculation to prevent nulling out the fulfiller's score if it's their first rating.
- **`rateCustomer` [NEW]**:
    - Implement logic to save `fulfiller_rating` and `fulfiller_comment` to the mission record.
    - Verify that the requester is the fulfiller assigned to the mission.

#### [MODIFY] [orderRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/orderRoutes.js)
- Register `POST /:orderId/rate` for `rateCustomer` (fulfiller rating customer).

---

### Android App

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Update the rating submission catch block to use `ErrorUtils.parseError(e)`. This will surface specific errors like "Already rated" instead of a generic failure message.

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Update the rating submission catch block to use `ErrorUtils.parseError(e)`.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend: `node -c src/controllers/orderController.js`.

### Manual Verification
1.  **Customer Rating:** Complete a mission, track it, and submit a 5-star rating.
2.  **Duplicate Check:** Try to submit a rating again for the same mission. The app should show "You've already rated this mission."
3.  **Fulfiller Rating:** As a fulfiller, complete a mission and submit a rating for the customer. Verify it saves successfully.
