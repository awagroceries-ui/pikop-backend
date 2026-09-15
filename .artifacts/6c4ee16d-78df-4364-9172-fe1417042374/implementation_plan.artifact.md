# Implementation Plan - Delivery Fee Audit & Payout Decoupling (Refined)

This plan confirms and fixes the implementation of Pikop's delivery fee rules: a universal 75/25 split and the immediate, decoupled release of fulfiller earnings upon delivery.

## 🔍 Diagnostic Report

### 1. 75/25 Split Confirmation
- **Implementation**: The split is centrally managed in `walletService.processMissionSettlement`. It correctly calculates a `platformShare` (25%) and `fulfillerShare` (75%).
- **Universality**: It applies to both standalone and Marketplace orders via the `delivery_fee` column. **Confirmed Universal.**

### 2. Payout Decoupling Confirmation
- **OTP Flow (`orderController.verifyDelivery`)**: Correctly calls `processMissionSettlement` for every order on delivery. **Confirmed Correct.**
- **Fulfiller Manual Flow (`orderController.updateStatus`)**: 🚨 **Gap Found.** Settlement is only triggered `if (!isEscrow)`. Fulfillers on Marketplace/COD orders are not being paid immediately.
- **Admin Manual Flow (`adminController.updateOrderStatus`)**: 🚨 **Gap Found.** Only calls `releaseEscrow` (item price). It **never** calls `processMissionSettlement`. Fulfillers are never paid the delivery fee portion in this flow.
- **Dispute Safety**: Verified that `releaseEscrow` and `refundEscrow` only touch item price/commissions. Fulfiller earnings are already protected. **Confirmed Correct.**

## Proposed Changes

### Backend Logic Fixes

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update `updateStatus` to trigger `walletService.processMissionSettlement(orderId)` for **all** orders when the status becomes `DELIVERED`, removing the `!isEscrow` condition.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `updateOrderStatus` to trigger `walletService.processMissionSettlement(id, client)` for **all** orders when the status becomes `DELIVERED`.
- This ensures fulfillers are always paid their delivery share, even during admin manual overrides.

## Verification Plan

### Manual Verification
1.  **Admin Force-Complete**: As an admin, force-complete a COD/Marketplace order. Verify the Fulfiller is paid their 75% share immediately.
2.  **Fulfiller Manual Complete**: Mark a non-escrow mission as delivered manually. Verify Fulfiller payout.
3.  **Dispute Test**: Buyer disputes item correctness. Verify Fulfiller's delivery share remains in their balance.
