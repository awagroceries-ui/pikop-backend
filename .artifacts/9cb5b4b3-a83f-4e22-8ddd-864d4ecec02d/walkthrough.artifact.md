# 🚀 Walkthrough: Mission Creation Fix & Order Summary Subtotal Calculation

Fixed the mission creation failure (`An unexpected error occurred. Please try again.`) caused by coupon UUID syntax errors, and corrected the order summary calculation so logistics subtotals never display negative values (`₦-2100.0`).

---

## 🛠️ Summary of Implementation

### 1. Backend Controllers Coupon Query Hardening
- Updated [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js):
  - Changed `SELECT * FROM coupons WHERE id = $1` to `SELECT * FROM coupons WHERE (id::text = $1 OR code ILIKE $1) AND is_active = true`.
  - Safely accepts promo code strings (such as `"TESTER100"`) without throwing PostgreSQL `22P02: invalid input syntax for type uuid: "TESTER100"`.
- Updated [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js) and [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js):
  - Applied the same `(id::text = $1 OR code ILIKE $1)` query hardening across checkout and activation handlers.

### 2. Mobile App Order Summary Subtotal Calculation
- Updated [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt):
  - Clamped `logisticsTotal = maxOf(0.0, deliveryFee - (if (isFullFreePromo) deliveryFee else discount)) + smsChargeVal`.
  - Ensures subtotal lines on the Order Summary breakdown never display negative values (e.g. `₦-2100.0`), cleanly rendering `₦0.0` for 100% free tester promos.

---

## 🧪 Device Verification & Deployment

- Built debug APK (`app:assembleDebug`) -> **`BUILD SUCCESSFUL`**.
- Installed and launched live on connected Wireless ADB device (**Samsung Galaxy S23 Ultra** @ `192.168.1.2:42447`).
- Changes staged, committed (`9d7b5a66`), and pushed to GitHub `origin/main`.
