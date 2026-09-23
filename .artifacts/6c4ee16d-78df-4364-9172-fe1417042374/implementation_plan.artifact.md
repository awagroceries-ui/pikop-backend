# Implementation Plan - 100% Universal Tester Coupon (`TESTER100`)

This plan introduces a 100% universal tester coupon (`TESTER100`) that bypasses real payment gateways across all four platform modules (Dispatch, Food, Groceries, Shop), enabling zero-cost end-to-end testing for testers and QA.

## Proposed Changes

### 1. Database Migration (Backend Seed)

#### [NEW] [1726870000000_seed_universal_tester_coupon.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726870000000_seed_universal_tester_coupon.js)
- Seed an active, unlimited-use coupon with code `TESTER100`:
  - `discount_type`: `'PERCENTAGE'`
  - `discount_value`: `100.00`
  - `min_order_amount`: `0.00`
  - `usage_limit`: `999999`
  - `is_active`: `true`

---

### 2. Backend Pricing & Activation Engines

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **100% Discount Rule**: Update `getQuote` and `createOrder` so that 100% percentage coupons discount the *entire* order fare (item price + delivery fee + platform fee).
- **Zero-Payment Activation**: When `finalFare === 0`, set the initial status to `'SEARCHING'` (or `'PENDING_ACKNOWLEDGMENT'` if recipient is app user), instantly activating the order without payment gateway redirection.

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- **Universal Commerce Discount**: In `initializeCommerceOrder`, if a 100% promo is applied, set `totalNaira = 0`.
- **Zero-Cost Checkout**: If `totalNaira === 0`, bypass Paystack initialization and immediately create the order as `PAID` / `SEARCHING` in the database, returning `{ success: true, order_id: newOrderId }`.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **Zero-Amount Guard**: In `initializePayment`, if the calculated amount is 0, activate the mission directly via `activatePaidMission()` and return `{ success: true, status: 'PAID', order_id: orderId }`.

---

### 3. Android Mobile Application

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Update discount calculation when `activePromo?.value >= 100.0`: set `amountToCharge = 0.0`.
- Ensure tapping "Place Order" when `amountToCharge == 0.0` uses the zero-upfront bypass route and activates the order immediately.

---

## User Review Required

> [!IMPORTANT]
> **Tester Coupon Code**
> The universal 100% code will be **`TESTER100`**. Entering this code in the promo/coupon field on any checkout screen will reduce the total payable amount to **₦0.00**.

> [!WARNING]
> **Escrow & Merchant Ledger**
> For testing marketplace/kitchen items with `TESTER100`, the item price will be ₦0.00. Escrow ledger entries will record ₦0.00 credit to the merchant wallet.

---

## Verification Plan

### Automated Tests
- Syntax check backend controllers using `node -c`.
- Build release app bundle / APK using Gradle.

### Manual Verification
1. **Dispatch Flow**:
   - Create a delivery quote in the app.
   - Enter promo code `TESTER100` and tap "Apply".
   - Confirm total payable drops to **₦0.00**.
   - Tap "Place Order" and confirm the order status moves directly to `SEARCHING` without launching Paystack.
2. **Commerce / Marketplace Flow**:
   - Select a Food, Grocery, or Shop item.
   - Enter `TESTER100` at checkout.
   - Confirm total drops to **₦0.00** and checkout succeeds instantly.
