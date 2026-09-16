# Implementation Plan - Fix Wallet Ledger Migration Violation

This plan resolves the migration error `check constraint "wallet_ledger_entries_purpose_check" is violated by some row` encountered on the production server.

## 🔍 Diagnostic Summary
- **Root Cause**: The migration `1726490000000_incident_management.js` attempts to add a strict `CHECK` constraint to the `purpose` column in the `wallet_ledger_entries` table. However, the list of allowed values in the migration missed several existing values used in previous versions of the app or in recently added features (e.g., `COD_COLLECTION`, legacy reward strings).
- **Violation**: Rows already exist in the database with these values, causing the `ADD CONSTRAINT` command to fail.

## Proposed Changes

### Backend (Migrations)

#### [MODIFY] [incident_management migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726490000000_incident_management.js)
- Update the `purpose` check constraint to include all known historical and current values:
    - Current v3: `SETTLEMENT`, `COMMISSION`, `SECURE_PAY_FEE`, `SMS_CHARGE`, `ESCROW_HOLD`, `ESCROW_RELEASE`, `ESCROW_REFUND`, `TOPUP`, `WITHDRAWAL`, `REFERRAL_BONUS`, `REFERRAL_WELCOME`, `CANCELLATION_PENALTY`, `BULK_DISPATCH`, `COD_COLLECTION`.
    - Incident Engine (New): `PENALTY_WAIVER`, `RETURN_FEE`, `RETURN_WAIVER`.
    - Legacy (Migration Support): `DELIVERY_PAYMENT`, `CANCELLATION_FEE`, `CORPORATE_ORDER`, `DIRECT_DEBIT_ORDER`, `REFERRAL_REWARD`, `REFEREE_WELCOME`.

## Verification Plan

### Manual Verification
1.  **Server Pull**: I will advise the user to pull the updated migration.
2.  **Migration Execution**: Run `npm run migrate:up` on the server. The constraint should now apply successfully as it covers all existing row data.

## User Action Required
> [!IMPORTANT]
> **Action Needed on Server**
> Please run the following commands on your production VPS once I've pushed the fix:
> ```bash
> cd /var/www/pikop-api/backend_v3/backend_v3
> git pull origin main
> npm run migrate:up
> pm2 restart pikop-v3
> ```
