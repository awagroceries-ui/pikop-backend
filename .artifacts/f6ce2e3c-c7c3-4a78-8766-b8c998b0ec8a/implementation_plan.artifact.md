# Implementation Plan - Fix COD Platform Fee Consistency & Checkout Display

This plan ensures that the 10% COD platform fee is applied correctly based on user roles and is displayed transparently in the order summary.

## User Review Required

> [!IMPORTANT]
> **Fee Payer Rule:** The Buyer (Payer of the item) always bears the 10% platform fee.
>
> **Initiator Billing:**
> - If the **Buyer** initiates the delivery, they pay `Item Price + Delivery Fee + Platform Fee + SMS Charge` at checkout.
> - If the **Seller** initiates the delivery, they pay only the `Delivery Fee + SMS Charge` at checkout. The Recipient (Buyer) will be sent a payment link for the `Item Price + Platform Fee`.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getQuote`**:
    - **Enforce Fee Payer:** Set `fee_payer = 'PAYER'` regardless of who initiates.
    - **Fix `total_payable`:** Calculate the amount the *initiator* must pay.
        - If `initiator_role === 'PAYER'`, include `item_price + platform_fee_amount`.
        - If `initiator_role === 'SELLER'`, exclude those from the upfront total.
    - **Rounding:** Update `PlatformConfig.roundFee` to ensure a minimum fee of ₦1 for any COD item.

#### [MODIFY] [PlatformConfig.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/config/platform.js)
- Update `roundFee` to: `(amount) => amount > 0 ? Math.max(1, Math.floor(amount)) : 0`.

---

### Android App

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- **Summary Logic:** Update the `amountToCharge` calculation to respect the `initiatorRole`.
- **UI Transparency:**
    - If the user is the **Seller**, show a clear breakdown of "Paid by You" vs. "Paid by Recipient".
    - Clearly label the "Platform fee" as being paid by the Payer (Buyer).

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Buyer Initiator Test:** Create a Secure Pay order as "Buyer". Verify "Total Payable" includes Item + Delivery + 10% Fee.
2.  **Seller Initiator Test:** Create a Secure Pay order as "Seller". Verify "Total Payable" includes ONLY Delivery Fee. Verify the recipient receives an SMS for `Item + 10% Fee`.
3.  **Boundary Test:** Set item price to ₦100. Verify platform fee is exactly ₦10.
4.  **Display Accuracy:** Verify the "Subtotal" line in the app matches the math (`item + fee`).
