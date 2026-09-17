# Implementation Plan - Marketplace Returns & Reverse Logistics

This plan adds a structured workflow for Marketplace returns occurring after COD escrow has closed, governed by merchant-specific policies.

## Proposed Changes

### 1. Database Schema Enhancements

#### [NEW] [marketplace_returns migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726530000000_marketplace_returns.js)
- **Extend `vendors` and `kitchens`**:
    - `allows_returns` (BOOLEAN, default false)
    - `return_window_days` (INTEGER, default 7)
    - `return_policy_text` (TEXT)
- **[NEW] `returns` table**:
    - `id` (UUID PRIMARY KEY, default `gen_random_uuid()`)
    - `order_id` (INT, references `orders`)
    - `status` (PENDING, APPROVED, DECLINED, IN_TRANSIT, RECEIVED, COMPLETED, CANCELLED)
    - `reason` (TEXT)
    - `evidence_urls` (TEXT[])
    - `merchant_notes` (TEXT)
    - `return_delivery_order_id` (INT, references `orders`)
    - `delivery_fee_payer` (VARCHAR, default 'CUSTOMER') - 'CUSTOMER' or 'MERCHANT'
    - `created_at` (TIMESTAMP)

### 2. Backend Logic (Node.js)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- `updateMerchantSettings`: Include return policy fields.
- `getReturnRequests`: Fetch pending/active returns for the merchant.
- `processReturnRequest`: Approve or decline a return.
    - On **Approve**:
        - Capture `delivery_fee_payer`.
        - Generate a reverse Dispatch mission (Pickup: Original Delivery Addr, Drop-off: Merchant Addr).
        - If `MERCHANT` pays: Status is `SEARCHING` (debit merchant wallet).
        - If `CUSTOMER` pays: Status is `AWAITING_PAYMENT`.
- `confirmReturnReceipt`: Triggered by merchant when item is back. Initiates the **Wallet Refund** of the original item price.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- `requestReturn`: Customer endpoint to initiate the flow from order history.

### 3. Payment & Refund Policy

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Implement `processReturnRefund(returnId)`: Refunds the **Item Price** from the Merchant's wallet back to the Customer's wallet.
- Handle Merchant debit for return delivery fee if they chose to absorb it.

### 4. Android App (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add DTOs for `ReturnRequest` and `ReturnResponse`.
- Add endpoints for requesting, approving, and confirming returns.

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Add a **Returns** tab to manage incoming requests.
- Update **Settings** tab to manage Return Policy.

#### [MODIFY] [OrdersDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrdersDashboardScreen.kt)
- Add "Request Return" action to completed Marketplace orders.

## User Review Required

> [!IMPORTANT]
> **Flexible Billing Implemented**
> Merchants now have a toggle when approving a return to "Cover Delivery Fee." If selected, the cost is debited from the Merchant's wallet, and the mission is instantly broadcasted. Otherwise, the Customer must pay to activate the return trip.

## Verification Plan

### Manual Verification
1.  **Merchant Pre-pays**: Approve a return as a Merchant and select "Merchant Pays". Verify the mission starts searching immediately.
2.  **Customer Pre-pays**: Approve a return and select "Customer Pays". Verify the mission is created but stuck in "Awaiting Payment" until the customer pays.
3.  **Full Cycle**: Complete the return delivery. Confirm receipt as Merchant. Verify the Customer receives the item price refund.
