# Implementation Plan - Production Fixes for Fulfiller Signup & Merchant Account Updates

This plan addresses two critical production issues preventing Fulfillers from completing signup and Merchants from updating their account settings.

## Proposed Changes

### 1. Fulfiller Signup Fix (Backend & DB Schema)

#### [NEW] [1726860000000_make_fulfiller_password_nullable.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726860000000_make_fulfiller_password_nullable.js)
- Create a migration to alter `fulfillers.password_hash` to be nullable (`notNull: false`). Since authentication relies on `users.password_hash`, requiring it in `fulfillers` causes a database constraint violation during registration.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Update `INSERT INTO fulfillers` in the `signup()` method to explicitly pass `passwordHash`. Combining this with the schema migration guarantees 100% resilience against registration failures.

---

### 2. Merchant Account Update Fix (Backend & API)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- **`updateMerchantSettings`**: Expand the endpoint to update `allows_returns`, `return_window_days`, `return_policy_text`, `business_name`, `category`, and `address` in addition to `accepts_cod` and `operating_hours`.
- **Slug Safety**: Ensure `store_slug` generation handles null or empty `business_name` safely using fallback strings to prevent runtime TypeErrors.

---

## User Review Required

> [!IMPORTANT]
> **Database Migration Required**
> Applying this fix on your server will require running `npm run migrate:up` to apply `1726860000000_make_fulfiller_password_nullable.js`.

---

## Verification Plan

### Automated Tests
- Build and verify backend syntax using `node -c` on all updated controllers and migration scripts.

### Manual Verification
1.  **Fulfiller Signup**: Register a new Fulfiller account in the app. Verify that signup succeeds, OTP is sent, and the `fulfillers` record is created without 500 errors.
2.  **Merchant Settings**:
    - Open Merchant Portal > Settings.
    - Toggle "Accept Cash on Delivery" and "Allow Marketplace Returns".
    - Change return window days and tap "Update Settings".
    - Refresh the dashboard to confirm settings persist in the database.
