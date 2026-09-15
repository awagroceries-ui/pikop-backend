# Walkthrough: Customized Welcome Emails

## Changes Made

### 1. Tailored Email Templating (`emailService.js`)
- Refactored `sendWelcomeEmail` into a robust dispatcher that serves five distinct user groups:
    - **Customer:** Overview of Dispatch, Food, Groceries, and Shop with a focus on COD/Escrow protection.
    - **Agent/Fulfiller:** Includes their specific mobility category (Foot Agent, Rider, Driver) and payout instructions.
    - **Food Merchant:** Explicitly states the **10%** commission and their COD preference.
    - **Groceries Merchant:** Explicitly states the **5%** commission (avoiding copy-paste drift).
    - **Shop Merchant:** Explicitly states the **10%** commission.
- **Dynamic Data:** All commission rates are pulled directly from `PlatformConfig` at send-time, ensuring they always match the system's live configuration.

### 2. Gated Triggering (`authController.js` & `adminController.js`)
- **Immediate for Customers:** Customers receive their welcome email immediately after successful OTP verification.
- **Approval-Gated for Partners:** Fulfillers and Merchants now only receive their welcome emails once an Admin officially marks their account as `VERIFIED` in the dashboard. This prevents onboarding emails from being sent to rejected applicants.
- Updated `updateKYCStatus` (Fulfillers) and implemented `updateMerchantKYCStatus` (Merchants) to trigger these emails upon approval.

### 3. Database Schema Support
- Added a migration `1726420000000_add_merchant_category.js` to store the business category ("Food", "Groceries", "Shop") directly on the `vendors` and `kitchens` tables.
- Updated the `setupMerchantProfile` logic to capture and save this category during onboarding.

## Verification Results
- **Rate Accuracy:** Confirmed that Groceries merchants receive a 5% rate mention while others receive 10%, mapped via `PlatformConfig`.
- **Navigation Safety:** All links to Terms, Privacy, and Support in the email templates have been verified.
- **Git State:** All changes committed and pushed to `main`.

## Deployment Instructions
To apply these backend changes and DB migrations to your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
