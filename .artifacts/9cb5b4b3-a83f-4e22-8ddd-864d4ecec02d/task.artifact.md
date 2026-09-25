# 📌 Task Checklist: Fix Mission Creation Failure & Negative Subtotal Display

- `[/]` Task 1: Backend Controllers Coupon UUID Query Hardening
  - `[ ]` Update coupon lookup query in `orderController.js` (`createOrder`) to `(id::text = $1 OR code ILIKE $1)`
  - `[ ]` Update coupon lookup query in `paymentController.js` (`initializePayment`, `verifyPayment`)
  - `[ ]` Update coupon lookup query in `commerceController.js` (Marketplace checkout)

- `[ ]` Task 2: Mobile App Order Summary Subtotal Fix (`OrderQuoteScreen.kt`)
  - `[ ]` Clamp Logistics Subtotal to `maxOf(0.0, deliveryFee - discount)` (never negative)
  - `[ ]` Pass `promo_id = activePromo?.id ?: activePromo?.code` in `CreateOrderRequest` and `PaymentInitializationRequest`

- `[ ]` Task 3: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 4: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
