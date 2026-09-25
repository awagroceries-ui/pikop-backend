# 📌 Task Checklist: Fix Mission Creation Failure & Negative Subtotal Display

- `[x]` Task 1: Backend Controllers Coupon UUID Query Hardening
  - `[x]` Update coupon lookup query in `orderController.js` (`createOrder`) to `(id::text = $1 OR code ILIKE $1)`
  - `[x]` Update coupon lookup query in `paymentController.js` (`initializePayment`, `verifyPayment`)
  - `[x]` Update coupon lookup query in `commerceController.js` (Marketplace checkout)

- `[x]` Task 2: Mobile App Order Summary Subtotal Fix (`OrderQuoteScreen.kt`)
  - `[x]` Clamp Logistics Subtotal to `maxOf(0.0, deliveryFee - discount)` (never negative)
  - `[x]` Pass `promo_id = activePromo?.id ?: activePromo?.code` in `CreateOrderRequest` and `PaymentInitializationRequest`

- `[x]` Task 3: Build & Deploy to Device
  - `[x]` Build debug APK (`app:assembleDebug`)
  - `[x]` Install and launch on device (`192.168.1.2:42447`)

- `[x]` Task 4: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
