# Implementation Plan - Fix COD Billing Responsibility

This plan corrects the billing logic for COD (Secure Pay) missions, ensuring the Buyer/Recipient is always responsible for all payments (Item + Delivery + Fees).

## User Review Required

> [!IMPORTANT]
> **New COD Billing Rule:**
> - **Buyer Initiates:** Buyer pays `Item + Fee + Delivery + SMS` upfront.
> - **Seller Initiates:** Seller pays **₦0** upfront. The Recipient (Buyer) receives an SMS link to pay the full `Item + Fee + Delivery + SMS` to activate the mission.
> - **Status Flow:** Missions initiated by sellers with ₦0 upfront will remain in `AWAITING_PAYMENT` status and will NOT be broadcast to fulfillers until the recipient completes the payment.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getQuote`**:
    - Update `total_payable` (for initiator) to be **0** if `initiator_role === 'SELLER'` and `item_price > 0`.
    - Calculate a new `recipient_total` which includes `item_price + platform_fee + delivery_fee + sms_charge`.
    - Return `recipient_total` in the API response.
- **`createOrder`**:
    - If `total_fare` is 0 (Seller-initiated COD), set initial status to `AWAITING_PAYMENT`.
    - Pass the full `recipient_total` to the `sendSecurePaySms` call.
- **`getGuestCheckout` [NEW]**:
    - A public EJS page at `/guest/checkout/:orderId`.
    - Displays the order breakdown (Item, Delivery, Fee, SMS).
    - Includes a "Pay Now" button that initializes Paystack for the full amount.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **`handleWebhook`**:
    - When a guest payment for a mission in `AWAITING_PAYMENT` succeeds, move status to `SEARCHING` to begin dispatch.

---

### Android App

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- **Summary Logic:**
    - Update `amountToCharge` to be `0.0` if `isSecurePay` and `initiatorRole == "SELLER"`.
- **UI Update:**
    - Change "Pay & Deploy" button text to **"Deploy & Request Payment"** when the charge is 0.
    - Explicitly show that the Recipient is paying the **Logistics (Delivery Fee)** in addition to the item price.

---

## Verification Plan

### Automated Tests
- Syntax check backend.
- Build Android app.

### Manual Verification
1.  **Seller-Initiated COD:**
    *   Create a COD mission as a Seller. Verify the app charges you **₦0**.
    *   Verify the mission is created on the server but is NOT visible to fulfillers yet.
    *   Verify the Recipient receives an SMS with a link for the **Full Total**.
2.  **Recipient Payment:**
    *   Open the link as the recipient. Complete payment.
    *   Verify the mission status moves to `SEARCHING` and becomes visible to fulfillers.
3.  **Buyer-Initiated COD:**
    *   Verify the Buyer is still charged the full amount upfront.
