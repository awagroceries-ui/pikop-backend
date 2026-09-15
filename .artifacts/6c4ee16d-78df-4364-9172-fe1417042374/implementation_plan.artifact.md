# Implementation Plan: Unified Checkout & Dispatch Automation

## 🔍 Diagnostic Report

I have investigated the backend and current frontend setups regarding checkout:
1. **Current State**: The `CommerceCheckoutScreen.kt` currently exists. It calls `/api/v1/commerce/checkout/initialize` in the backend (`commerceController.js`), which triggers a Paystack transaction with `metadata.type = 'COMMERCE_ORDER'`.
2. **Current Automation**: When Paystack succeeds, `paymentController.js` handles the webhook, intercepts `COMMERCE_ORDER`, and **already natively creates an `orders` table entry** with `order_type='pickup_delivery'` and `status='SEARCHING'`. It sets the pickup coordinates to the merchant's address and the delivery coordinates to the customer's address!
3. **Escrow logic**: The webhook already routes the `item_price` portion into `ESCROW_HOLD` for the merchant.
4. **The Missing Piece (COD)**: The prompt asks to ensure COD works exactly as it does for Dispatch orders. Currently, `CommerceCheckoutScreen.kt` has *no* UI for selecting payment methods (it just instantly triggers Paystack). The backend `initializeCommerceOrder` endpoint also assumes immediate card payment.

## User Review Required

> [!IMPORTANT]
> **COD Architecture for Commerce**
> Since the backend webhook is already fully automating the Dispatch job creation for paid commerce orders, my primary task is updating the UI and API to allow **Cash on Delivery (COD)** as an option during unified checkout.
>
> I will:
> 1. Update `CommerceCheckoutScreen.kt` to allow users to toggle between "Pay Now (Card/Transfer)" and "Cash on Delivery".
> 2. Update `initializeCommerceOrder` in `commerceController.js` to accept a `payment_method` (e.g. `CARD` vs `COD`).
> 3. If `payment_method = 'COD'`, bypass Paystack. Instead, directly insert the order into the database exactly like the webhook does, but set `payment_status = 'PENDING'` and `collection_status = 'pending'`, calculating the total COD amount.
> 4. Ensure the resulting order ID is returned so the customer can track it immediately.
>
> Do you approve of this approach?

## Proposed Changes

### Part 1: Android Frontend (`CommerceCheckoutScreen.kt`)

#### [MODIFY] [CommerceCheckoutScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/CommerceCheckoutScreen.kt)
- Introduce a Payment Method radio toggle (`Card / Transfer` vs `Cash on Delivery`).
- Update the `CommerceOrderRequest` data class in `ApiService.kt` to include `payment_method: String`.
- If Pay Now is selected, open the Paystack URL as before.
- If COD is selected, `initializeCommerceOrder` will return `{ success: true, order_id: "..." }` instead of a payment URL. Automatically route the user to the `track_order` screen for that ID.

### Part 2: Backend Unified Initialization (`commerceController.js`)

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- Update `initializeCommerceOrder`.
- Accept `payment_method` in `req.body`.
- **If `payment_method === 'COD'`**:
  - Instead of initializing Paystack, insert directly into the `orders` table.
  - `status = 'SEARCHING'`.
  - `payment_status = 'PENDING'`.
  - `collect_on_delivery_amount = totalNaira`.
  - Fetch nearby fulfillers and broadcast the offer immediately (reuse `dispatchService.broadcastOffer`).
  - Return the new `order.id` to the frontend.

### Part 3: Linkage Visibility

- The linkage requested ("customer looking at Food order should see delivery tracking") is naturally solved because Pikop v3 uses a unified `orders` table. A commerce order *is* a dispatch order natively. The `OrdersDashboardScreen` and `TrackOrderScreen` already query the `orders` table, meaning commerce purchases will automatically appear in the user's mission history and can be tracked natively.

## Verification Plan

### Manual Verification
1. Add an item to the cart in the Food module and proceed to checkout.
2. Select "Cash on Delivery". Submit order.
3. Verify that it bypasses Paystack, returns an `order_id`, and takes you straight to the Tracking Screen.
4. Check the Fulfiller app -> Verify the mission is broadcast to nearby drivers.
5. Check the Merchant app -> Verify the incoming order appears in the "Orders" tab.