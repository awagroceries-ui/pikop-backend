# Implementation Plan - Decouple Fulfiller Earnings from COD Release

This plan ensures fulfillers are paid for their delivery service immediately upon drop-off, even if the COD item payment is still held in escrow for the seller.

## User Review Required

> [!IMPORTANT]
> **Payout Split Logic:**
> 1. The fulfiller's **75% delivery fee share** will be credited to their **Available Balance** immediately when they verify the delivery code.
> 2. The **Item Price** (if applicable) will remain in the **Seller's Pending Balance** until the customer confirms receipt or the grace period expires.
>
> **Seller ID mapping:** I will update the system to correctly identify whether the Agent (Fulfiller) or a separate User (Vendor/Customer) is the recipient of the escrowed item payment.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update `getOrderDetails` to include `seller_id` in the selection and response.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- **`releaseEscrow`**:
    - If `seller_id` exists, release the item price to the `USER` wallet associated with that ID.
    - If `seller_id` is null, fall back to the `FULFILLER` wallet (for independent agents).
- **`refundEscrow`**: Update similarly to target the correct `pending_balance` pool.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `seller_id: Int?` to `OrderDetailsResponse`.

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Update the `isAwaitingRelease` UI:
    - **Header:** Change to "Mission Successfully Completed!"
    - **Earning Highlight:** Add a line: "₦[Earning Amount] has been credited to your available balance."
    - **Escrow Note:**
        - If the agent is the seller: "The item payment (₦[Price]) is pending customer confirmation."
        - If the agent is NOT the seller: "The item payment will be released to the seller once the customer confirms."

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c src/services/walletService.js`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **COD Mission (Agent != Seller):**
    *   Complete delivery as an agent.
    *   Verify available balance increases by 75% of delivery fee immediately.
    *   Verify UI shows "Earnings Credited".
2.  **COD Mission (Agent == Seller):**
    *   Complete delivery.
    *   Verify available balance increases by delivery fee share.
    *   Verify pending balance reflects the item price.
    *   Verify UI shows "Item Payment Pending".
