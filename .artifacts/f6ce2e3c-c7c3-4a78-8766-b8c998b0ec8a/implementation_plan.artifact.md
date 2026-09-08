# Implementation Plan - Fix Fulfiller History, Earnings & Free Mission Payouts

This plan addresses the missing fulfiller history, zero wallet earnings, and ensures fulfillers are paid correctly for promo-discounted missions.

## Problem Description
1.  **Empty Fulfiller History:** Fulfillers are unable to see their completed or past missions in the history tab.
2.  **Missing Wallet Earnings:** Completed missions are not resulting in wallet credits.
3.  **Free Mission Payouts:** Fulfillers receive ₦0 for missions where the customer used a 100% discount promo code, even though they are entitled to 75% of the original fare.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725584000000_add_payout_data_to_orders.js)
- Add `original_delivery_fee` and `original_total_fare` columns to the `orders` table. This preserves the pre-discount pricing for payout calculations.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`createOrder`**:
    - Save the quote's original `delivery_fee` and `total_fare` into the new `original_` columns.
    - Set `status` to `PAYMENT_CAPTURED` for promo missions.
- **`getFulfillerOrders`**:
    - Add diagnostic logging to track result counts.
    - Calculate `earnings` in the SQL query: `ROUND(COALESCE(o.original_delivery_fee, o.delivery_fee, o.total_fare) * 0.75, 2) as earnings`.
- **`rateFulfiller`**: Fix a potential query error by adding `user_id` check.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **`activatePaidMission`**: Populate the new `original_` columns from the quote record.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- **`processMissionSettlement`**:
    - Change payout calculation to use `original_delivery_fee` if available.
    - Ensure fulfillers are credited 75% of the original delivery cost even if the customer paid ₦0.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `CreateOrderRequest` to include `delivery_fee`.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Pass `delivery_fee = result.delivery_fee` in the `CreateOrderRequest` for free missions.

---

## Verification Plan

### Automated Tests
- Run Android build: `./gradlew assembleDebug`.
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **Free Mission Payout:**
    *   Apply a 100% promo code.
    *   Complete the mission as a fulfiller.
    *   Check fulfiller wallet: It should be credited with 75% of the original delivery fee.
2.  **Fulfiller History:**
    *   Open "Missions" tab as fulfiller.
    *   Verify all past missions are listed with their correct earnings.
3.  **Customer Rating:**
    *   Verify the rating prompt appears immediately after release.
