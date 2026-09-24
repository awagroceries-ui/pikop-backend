# 📋 Implementation Plan: Account Self-Deletion Transaction Fix & Missing Column

Address the continuing account deletion failures caused by transaction aborts (due to missing SAVEPOINTs during cleanup) and the `column "kyc_provider_ref" of relation "users" does not exist` PostgreSQL schema error.

---

## 🔍 Root Cause Analysis

1. **Transaction Abort Error in `deleteAccount`**:
   - In `authController.js` (`deleteAccount`), user account deletion/anonymization cascades optional cleanup tasks using `.catch(() => {})` inside an active PostgreSQL transaction block (`BEGIN`).
   - If an optional cleanup query fails (e.g., trying to access a table or column that isn't fully migrated, or foreign key violations), PostgreSQL instantly marks the main transaction as `ABORTED`. All subsequent commands are ignored until `ROLLBACK`. Catching the promise error in Node.js does not recover the SQL transaction state.
   - **Fix**: Replicate the `safeExec` `SAVEPOINT` pattern (introduced previously in `adminController.js`) inside `authController.js`.

2. **Missing `kyc_provider_ref` Column on `users` Table**:
   - The OTP verification flow (Termii SMS) uses `kyc_provider_ref` on the `users` table to store the `pinId` (`UPDATE users SET kyc_provider_ref = ...`).
   - While `kyc_provider_ref` was previously added to the `fulfillers` table (for Prembly/Didit), it was never formally migrated onto the core `users` table.
   - The `deleteAccount` anonymization query explicitly attempts to `SET kyc_provider_ref = NULL` on the `users` table, triggering `column "kyc_provider_ref" does not exist` and instantly crashing the transaction.
   - **Fix**: Create a migration to add `kyc_provider_ref` to `users`.

---

## 🛠️ Proposed Changes

### Component 1: Database Migration

#### [NEW] [1726930000000_add_kyc_provider_ref_to_users.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726930000000_add_kyc_provider_ref_to_users.js)
- Add column `kyc_provider_ref` (`varchar(255)`) to `users` table.

---

### Component 2: Backend Controller Fix

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Add `safeExec` helper function at the top of the file to execute queries within `SAVEPOINT` wrappers.
- Update `deleteAccount`:
  - Replace `.catch(() => {})` cleanup queries with `safeExec`.
  - Ensure the main `UPDATE users ... SET kyc_provider_ref = NULL` executes safely now that the column exists.

---

## 🧪 Verification Plan

1. Run database migration (`npm run migrate:up`).
2. Verify the `users` table contains the `kyc_provider_ref` column.
3. Test Account Deletion (Self-Delete) from the Mobile App:
   - Go to Profile Settings -> Delete Account.
   - Proceed with deletion on an account with a ₦0.00 wallet balance.
   - Verify success response (`200 OK`) and that the user session is revoked, with no 500 transaction abort errors on the backend.
4. Git commit and restart VPS server.