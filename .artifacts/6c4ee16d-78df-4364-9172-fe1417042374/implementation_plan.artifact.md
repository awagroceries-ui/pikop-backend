# Implementation Plan - Fix Merchant Signup Database Constraint Error

This plan resolves the "value too long for type character varying(20)" error encountered during Merchant signup business verification.

## 🔍 Diagnostic Summary
- **Root Cause**: The `status` column in the `vendors` and `kitchens` tables is defined as `VARCHAR(20)`. However, the `setupMerchantProfile` function in `merchantController.js` attempts to insert the status `'pending_business_verification'`, which is 29 characters long.
- **Affected Tables**: `vendors`, `kitchens`.
- **Affected Column**: `status`.

## Proposed Changes

### 1. Database Schema Update
#### [NEW] [extend_merchant_status_length migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726460000000_extend_merchant_status_length.js)
- Alter the `status` column in `vendors` table to `VARCHAR(50)`.
- Alter the `status` column in `kitchens` table to `VARCHAR(50)`.
- (Optional but recommended) Alter the `status` column in `fulfillers` and `users` tables to `VARCHAR(50)` for future-proofing and consistency.

## Verification Plan

### Automated/Code Verification
- Verify the migration file uses correct syntax for altering column types in `node-pg-migrate`.

### Manual Verification
1.  **Apply Migration**: Run `npm run migrate:up` on the server.
2.  **Test Signup**: Attempt to complete the Merchant Business Verification step again.
3.  **Confirm Success**: Verify the profile is created and the status is correctly set to `pending_business_verification`.

## User Action Required
> [!IMPORTANT]
> **Action Needed on Server**
> Once these changes are pushed, you must run the following on your production VPS:
> ```bash
> cd /var/www/pikop-api/backend_v3/backend_v3
> git pull origin main
> npm run migrate:up
> pm2 restart pikop-v3
> ```
