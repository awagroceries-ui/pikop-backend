# Task Checklist: Unified Checkout & Dispatch Automation

- `[/]` **Part 1: API Model Updates**
  - `[ ]` Update `CommerceOrderRequest` in `ApiService.kt` to include `payment_method`.
  - `[ ]` Update `PaymentInitializationResponse` to handle conditional `order_id` vs `authorization_url`.
- `[ ]` **Part 2: Frontend UI (`CommerceCheckoutScreen.kt`)**
  - `[ ]` Add Payment Method Radio Buttons (Pay Now vs Cash on Delivery).
  - `[ ]` Handle conditional routing (Start Intent if Paystack URL exists, else navigate to `track_order/$orderId` if COD).
- `[ ]` **Part 3: Backend Logic (`commerceController.js`)**
  - `[ ]` Read `payment_method` in `initializeCommerceOrder`.
  - `[ ]` If `COD`: Insert directly into `orders` with `payment_status='PENDING'`, calculate total COD amount, broadcast to fulfillers, return order ID.
- `[ ]` **Part 4: Verification & Git**
  - `[ ]` Compile project to verify Android code syntax.
  - `[ ]` Git commit and push changes.