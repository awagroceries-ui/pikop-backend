# Implementation Plan - Signup Restoration & Transaction Resilience

This plan fixes the critical 500 error during signup caused by transaction poisoning and database constraint mismatches.

## 🔍 Diagnostic Summary
1.  **Transaction Poisoning (Critical)**: In `authController.js`, if the `user_legal_consents` table is missing (migration not run), the caught error inside the transaction puts Postgres into an "aborted" state. This causes the subsequent `COMMIT` to fail, resulting in a 500 error for the entire signup.
2.  **Missing Role**: The `users` table check constraint lacks the `MERCHANT` role, causing registration for merchants to fail.
3.  **Case Mismatch**: Fulfiller registration sends uppercase classes (e.g., `RIDER`), but the DB constraint only accepts lowercase (`rider`).
4.  **Error Visibility**: The signup error handler doesn't log the specific error details, making debugging difficult.

## Proposed Changes

### 1. Database Schema Fixes
#### [NEW] [fix_signup_constraints migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726450000000_fix_signup_constraints.js)
- Update `users` table constraint to include `MERCHANT`.
- Update `fulfillers` table constraint to be case-insensitive or include uppercase variants. *Decision: I will use case-insensitive check or just support both for safety.*

### 2. Backend Logic Restoration
#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Move `user_legal_consents` recording **after** the transaction `COMMIT`. This ensures the user is created even if the consent logging fails.
- Change Fulfiller `primary_class` to lowercase before insertion to match existing DB conventions.
- Enhance error logging in the `catch` block to print the full error stack for easier VPS debugging.

## Verification Plan
1.  **Code Audit**: Ensure no database queries are caught and swallowed *inside* a transaction block without a rollback.
2.  **Manual Test**:
    - Attempt a Customer signup.
    - Attempt a Merchant signup.
    - Attempt a Fulfiller signup.

## User Action Required
> [!IMPORTANT]
> **Action Needed on Server**
> After I push these changes, you MUST run:
> ```bash
> cd /var/www/pikop-api/backend_v3/backend_v3
> git pull origin main
> npm run migrate:up
> pm2 restart pikop-v3
> ```
