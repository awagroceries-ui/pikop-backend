# 📋 Implementation Plan: Fix Mission Creation Failure & Negative Subtotal Display

Fix the unexpected mission creation error (`An unexpected error occurred. Please try again.`) and correct the order summary calculation so logistics subtotals never display negative values (`₦-2100.0`).

---

## 🔍 Root Cause Analysis

1. **Backend Coupon UUID Lookup Error**:
   - In `orderController.js` (`createOrder`), `paymentController.js`, and `commerceController.js`, coupons were queried with:
     `SELECT * FROM coupons WHERE id = $1 AND is_active = true`
   - When a user applied a promo code string (e.g., `"TESTER100"`), passing `"TESTER100"` as `$1` caused PostgreSQL to crash with:
     `22P02: invalid input syntax for type uuid: "TESTER100"`
   - This triggered an unhandled 500 error on the server, resulting in the mobile app error message:
     `An unexpected error occurred. Please try again.`

2. **Negative Subtotal Rendering in App**:
   - In `OrderQuoteScreen.kt`, applying a `100%` promo discount (`₦3600.0`) subtracted the discount directly from the `Delivery Fee` (`₦1500.0`), yielding a negative subtotal (`1500 - 3600 = -2100`), which rendered as `Logistics Subtotal (You pay): ₦-2100.0`.

---

## 🛠️ Proposed Changes

### Component 1: Backend API Controllers (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update coupon query in `createOrder` to match on either `id::text` or `code`:
  `SELECT * FROM coupons WHERE (id::text = $1 OR code ILIKE $1) AND is_active = true`
- Save `couponId = c.id` (the valid UUID) into `orders.coupon_id`.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Update coupon query in `initializePayment` and `verifyPayment` to query `(id::text = $1 OR code ILIKE $1)`.

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- Update coupon query in marketplace checkout to query `(id::text = $1 OR code ILIKE $1)`.

---

### Component 2: Android Mobile App (`:app`)

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Fix Order Summary breakdown calculation:
  - `logisticsSubtotal = maxOf(0.0, deliveryFee - discount)`
  - Prevents negative logistics subtotal rendering (`₦-2100.0`).
- Pass `promo_id = activePromo?.id ?: activePromo?.code` in `CreateOrderRequest` and `PaymentInitializationRequest`.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Test mission creation with `TESTER100` coupon code on connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
2. Verify Order Summary renders clean positive subtotals (`₦0.0` for 100% promo) without negative numbers.
3. Tap **"Pay & Deploy Mission"** -> verify mission is activated successfully with `SEARCHING` / `PENDING_ACKNOWLEDGMENT` status.
4. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to device.
5. Stage, commit, and push changes to GitHub `main`.
6. Deploy to production VPS server (`api.pikop.com.ng`) and restart PM2.
