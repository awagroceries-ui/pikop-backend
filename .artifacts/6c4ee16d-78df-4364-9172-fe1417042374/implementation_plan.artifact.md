# Implementation Plan - Audit-Driven Fixes & Hardening

This plan addresses hidden bugs, data integrity risks, and idempotency gaps discovered during the comprehensive audit of recent features.

## Proposed Changes

### 1. Financial Hardening (Backend)

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- **Idempotency**: Update `processMissionSettlement`, `releaseEscrow`, and `processReferralReward` to check the `wallet_ledger_entries` table for existing transactions linked to the same `order_id` and `purpose`. This prevents duplicate payouts if a process is triggered twice.
- **Ledger Constraints**: Ensure `INSURANCE_PREMIUM` and `INSURANCE_CLAIM` are always recorded with the `order_id` for auditability.

### 2. State Machine Consistency (Backend)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **[NEW] `rescheduleOrder`**: Allow users to update `scheduled_at` if the mission is still in `SCHEDULED` status.
- **Dispute Resolution**: Add logic to `resolveDispute` (in `adminController.js` calling `walletService.js`) to ensure that if a dispute is settled via refund, the `escrow_status` is updated to `refunded` and the `status` to `REFUNDED` in a single transaction.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- **Role Awareness**: Update `updateMerchantKYCStatus` to handle users who might have multiple capabilities. Instead of overwriting `role`, consider a bitmask or a separate `capabilities` table (future-proofing). For now, I will add a check to see if the user is already a `FULFILLER` before changing their role.

### 3. UI/UX Polishing (Android)

#### [MODIFY] [SupportHubScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SupportHubScreen.kt)
- **Counts**: Add a badge to category accordions showing the number of articles in that section.
- **Empty States**: Improve the "No search results" view with a clear "Clear Search" button.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- **Validation Feedback**: If a scheduled time is blocked due to security windows or merchant hours, show an Inline Error Message rather than just a Toast, so the user knows exactly why they can't proceed.

## User Review Required

> [!IMPORTANT]
> **Idempotency Implementation**
> I am adding unique constraints or lookups on `(order_id, purpose)` in the ledger for certain transaction types. This is the safest way to prevent "Ghost Payouts" if the server or a job retries a settlement.

## Verification Plan

### Manual Verification
1.  **Double Settlement**: Trigger `processMissionSettlement` manually via a script twice for the same order. Verify only one ledger entry is created.
2.  **Role Preservation**: Verify that a `FULFILLER` who gets approved as a `MERCHANT` doesn't lose their ability to fulfill missions (or at least, the system warns the admin).
3.  **Rescheduling**: Create a scheduled order, then change the time. Verify the database updates and the background job respects the new time.
