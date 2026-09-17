# Implementation Plan - In-App Wallet Checkout

This plan enables Customers to pay for deliveries and marketplace orders using their existing Pikop Wallet balance, providing a zero-friction alternative to Paystack for funded accounts.

## Proposed Changes

### 1. Backend Logic (Node.js)

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Implement `processIndividualWalletPayment(userId, amount, orderId)`:
    - Atomically checks if the user's `available_balance` is sufficient.
    - Performs a `DEBIT` with `purpose: 'MISSION_PAYMENT'`.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update `createOrder`:
    - Handle `payment_method === 'wallet'`.
    - If selected, trigger `processIndividualWalletPayment`.
    - Mark `payment_status = 'PAID'` and status = `SEARCHING` (or `SCHEDULED`) immediately.

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- Update `initializeCommerceOrder`:
    - Add support for `payment_method === 'wallet'`.
    - Similar to the COD flow, but performs an immediate debit and sets `payment_status = 'PAID'`.

---

### 2. Android App Integration (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `CommerceOrderRequest` to support `WALLETPAY` (to differentiate from `CARD` and `COD`).

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Fetch user wallet balance on screen load.
- Add **"My Wallet"** as a billing option.
- **UI Logic**:
    - Show available balance on the selector.
    - Only allow selection if `balance >= total_fare`.
- On confirmation, skip Paystack browser and call `createOrder` with `payment_method: 'wallet'`.

#### [MODIFY] [CommerceCheckoutScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/CommerceCheckoutScreen.kt)
- Add **"Pay from Wallet"** card alongside "Pay Now" and "COD".
- Implement similar balance check and direct-activation logic.

---

## User Review Required

> [!IMPORTANT]
> **Simple Full-Payment Model**
> For this first version, I am implementing a **Full-or-Nothing** model. Users cannot split payment between Wallet and Card. They must have the full amount in their wallet to see the option.

## Verification Plan

### Manual Verification
1.  **Funded Checkout**: Top up a wallet to ₦5,000. Create an order for ₦1,500. Select "My Wallet". Verify the order activates immediately and the wallet balance is now ₦3,500.
2.  **Insufficient Balance**: Create an order for ₦5,001. Verify the "My Wallet" option is either disabled or shows a "Low Balance" warning and cannot be selected.
3.  **Marketplace Test**: Purchase a ₦2,000 item using Wallet. Verify the merchant receives the sale notification and the customer balance decreases correctly.
