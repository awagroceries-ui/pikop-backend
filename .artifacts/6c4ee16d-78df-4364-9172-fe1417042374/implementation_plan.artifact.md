# Implementation Plan - Total User Base Reset

This plan describes the process to delete all existing user accounts (Customers, Fulfillers, and Merchants) and their associated data to allow for a clean system restart.

## User Review Required

> [!CAUTION]
> **DESTRUCTIVE ACTION**
> This process will permanently delete all user profiles, mission history, wallet balances, and merchant listings. This action cannot be undone.

## Proposed Changes

### 1. Database Cleanup (SQL)

I will create a single migration file `1726850000000_total_user_reset.js` that executes a cascaded deletion of all user-related data.

#### [NEW] [Reset Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726850000000_total_user_reset.js)
The script will perform the following operations in a transaction:
- Truncate all transactional tables: `wallet_ledger_entries`, `order_items`, `orders`, `disputes`, `returns`, `emergency_alerts`, `sms_logs`, `fcm_logs`.
- Truncate all profile/identity tables: `fulfillers`, `vendors`, `kitchens`, `products`, `menu_items`, `kyc_documents`.
- Truncate all account management tables: `user_sessions`, `otp_verifications`, `referrals`, `loyalty_ledger`, `corporate_sub_accounts`, `corporate_accounts`, `merchant_sub_accounts`, `merchant_accounts`.
- Delete all records from the `users` table where `role != 'ADMIN'` and `role != 'SUPER_ADMIN'`.

### 2. File System Cleanup (Optional/Cleanup)
- Clear the `uploads/` directory on the server to remove old KYC documents and product photos.

---

## Verification Plan

### Manual Verification
1.  **Run Migration**: Execute `npm run migrate:up` on the server.
2.  **Verify Empty State**:
    - Log in to the Admin Dashboard.
    - Confirm "Total Users" is 0 (or only shows Admin accounts).
    - Confirm "Active Orders" and "Total Products" are 0.
3.  **Registration Test**: Attempt to sign up a new Customer and a new Merchant to ensure the sequences and constraints still work perfectly.
