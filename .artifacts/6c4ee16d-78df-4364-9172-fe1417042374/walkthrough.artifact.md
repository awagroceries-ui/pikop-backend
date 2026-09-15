# Walkthrough: Unified Commerce Checkout

## Changes Made
1. **Frontend UI Update (`CommerceCheckoutScreen.kt`)**:
   - I added a clean, visual `Payment Method` selector, allowing the user to choose between **"Pay Now (Card/Transfer)"** and **"Pay on Delivery (Cash/Transfer)"**.
   - Updated the `CommerceOrderRequest` data class in the API layer to support transmitting the chosen `payment_method`.
   - Adapted the checkout button logic: If the user picks Card, the app starts the Paystack intent window (status quo). If the user picks COD, the app seamlessly redirects them straight to the `TrackOrderScreen` for their newly created background mission!
2. **Backend Engine Update (`commerceController.js`)**:
   - Updated `initializeCommerceOrder` to read the `payment_method` variable.
   - If `payment_method === 'COD'`, the engine completely bypasses the Paystack initialization. Instead, it natively mimics the webhook's automation:
     - It creates a new `pickup_delivery` row in the `orders` table.
     - Sets `status = 'SEARCHING'`, `payment_status = 'PENDING'`, and `collection_status = 'pending'`.
     - Fills in `collect_on_delivery_amount`.
     - Secures the item price portion in `ESCROW_HOLD` for the merchant.
     - Extracts the nearby fulfillers and instantly broadcasts the mission via Socket.io.
   - It then returns the new `order_id` to the app so the customer can track the Dispatch mission.

## Result
Because Pikop's architecture leverages a unified `orders` table, this means:
- The customer can buy Food, Groceries, or Shop items and checking out *automatically* orchestrates the Dispatch lifecycle.
- The merchant can view the incoming order, accept it, and mark it ready for pickup.
- The fulfiller sees the mission pop up organically on their map, fully linked.

## Build and Testing Status
- The Android project compiles smoothly (`:app:assembleDebug`).
- The syntax validation passes on the backend endpoints.
- All code has been successfully committed to version control and pushed to your `main` repository!