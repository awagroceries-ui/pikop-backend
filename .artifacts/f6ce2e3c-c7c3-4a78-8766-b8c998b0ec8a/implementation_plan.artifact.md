# Implementation Plan - Pikop Commerce Phase 4: Unified Checkout

This final phase integrates the storefront with the platform's payment and dispatch engines, enabling a seamless "Buy and Deliver" experience for users.

## User Review Required

> [!IMPORTANT]
> **Automated Logistics:** When a commerce order is placed, the system will automatically create a delivery mission from the **Merchant's pickup point** to the **Customer's delivery address**. Fulfillers will be dispatched the moment the payment is confirmed.
>
> **Escrow Protection:** The item cost will be held by Pikop and only released to the Merchant after the customer confirms receipt in the app.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration: link_orders_to_commerce.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725630000000_link_orders_to_commerce.js)
- Add the following columns to the `orders` table:
    - `vendor_id` (UUID)
    - `kitchen_id` (UUID)
    - `product_id` (Int)
    - `menu_item_id` (Int)
- This ensures every commerce order remains linked to its source listing and merchant for audit purposes.

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- **`initializeCommerceOrder` [NEW]**:
    - Calculates the total cost: `Item Price` + `Automated Delivery Fee` + `Platform Fees`.
    - Returns a Paystack authorization URL.
    - Includes `commerce_data` in the payment metadata (item ID, type, address info).

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Update the webhook handler to detect `type: 'COMMERCE_ORDER'`.
- Automatically creates a `pickup_delivery` mission once the payment is successful.
- Credits the item price to the platform escrow, marked for the specific merchant.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `initializeCommerceOrder(request: CommerceOrderRequest): PaymentInitializationResponse`.

#### [NEW] [feature/commerce] [CommerceCheckoutScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/CommerceCheckoutScreen.kt)
- **Order Summary:** Item photo, name, and quantity.
- **Delivery Selection:** Integrated address picker (Saved Addresses or Map).
- **Price Breakdown:** Clear view of Item Cost vs. Delivery Fee.
- **Payment:** Direct link to Paystack WebView.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Register the `commerce_checkout` route.
- Connect `StorefrontScreen` clicks to the new checkout flow.

---

## Verification Plan

### Manual Verification
1.  **Price Check:** Select a ₦5,000 item. Select a delivery address 10km away. Verify the checkout total correctly shows ₦5,000 + the distance-based delivery fee.
2.  **Purchase Flow:** Complete a test payment via Paystack.
3.  **Auto-Dispatch:** Verify that a new delivery mission is **automatically created** and appears on the nearby Agent's dashboard.
4.  **Escrow Release:** Confirm the delivery in the app. Verify the Merchant's wallet is credited the item price (minus fees).
