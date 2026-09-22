# Walkthrough - Production Fixes for Fulfiller Signup & Merchant Settings

I have resolved the two production issues preventing Fulfiller registration and Merchant account setting updates.

## Changes Made

### 🚴 1. Fulfiller Signup Fix
- **Database Schema**: Created migration `1726860000000_make_fulfiller_password_nullable.js` to alter `fulfillers.password_hash` to `NULL`. Since user authentication is handled via `users.password_hash`, requiring a duplicate non-null password hash in `fulfillers` was causing PostgreSQL to reject initial registrations with a `NOT NULL constraint violation`.
- **Signup Controller**: Updated `authController.js` to explicitly pass `passwordHash` into `fulfillers` upon account creation as an additional safeguard.

### 🏪 2. Merchant Account Updates
- **Settings Endpoint Expanded**: Updated `updateMerchantSettings` in `merchantController.js` to handle all setting parameters sent by the mobile app (`allows_returns`, `return_window_days`, `return_policy_text`, `business_name`, `category`, and `address`).
- **Null-Safe Store Slugs**: Added fallback handling to `setupMerchantProfile` and `updateMerchantSettings` so empty business names won't cause runtime `TypeErrors` during slug generation.

---

## Verification Results

- **Syntax Validation**: [VERIFIED] All modified controller files (`authController.js`, `merchantController.js`) and the new migration script passed Node syntax checks (`node -c`) cleanly.

---

## Deployment Instructions

1.  **Push Changes from Android Studio**:
    Run `git push` in your local terminal (or push via Android Studio's Git menu) to upload the commits.

2.  **Apply Migration on VPS**:
    Run the following on your VPS server to apply the schema fix and restart the API:
    ```bash
    cd /var/www/pikop-api/backend_v3/backend_v3
    git pull origin main
    npm run migrate:up
    pm2 restart pikop-v3
    ```
