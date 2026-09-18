# Walkthrough - Fix Merchant Product Creation

I have resolved the issue where Merchants were unable to create products by addressing legacy account gaps and improving verification status transparency.

## Changes Made

### 🔧 1. Legacy Merchant Repair
- **Smart Routing**: Updated the **"Manage My Shop"** button in `AccountScreen.kt`. It now performs a real-time check for a valid merchant profile record.
- **Onboarding Bridge**: If a user has the `MERCHANT` role but is missing their business profile (common for accounts created via the old legacy path), the app now automatically routes them to the **Business Setup** screen instead of showing a broken portal.

### 🛡️ 2. Verification Gating (Backend & UI)
- **Status Banners**: Added a clear "Verification Pending" notice in the **Seller Center**. This informs unapproved merchants that their profile is under review and that listing items is temporarily disabled.
- **FAB Gating**: The "Add Item" button is now dynamically hidden unless the merchant's status is officially **'active'**.
- **Backend Enforcement**: Added server-side status checks to the `addProduct` and `addMenuItem` endpoints. Even if the UI is bypassed, the API will block listings from unverified accounts.

### 📜 3. Data Integrity
- **Full Profile Sync**: Updated `getMerchantProfile` on the backend to return all business fields, allowing the app to make better logic decisions based on category and verification stage.
- **DTO Alignment**: Synchronized the Android `MerchantProfile` model with the latest backend schema.

## Verification Results
- **Legacy Repair**: [VERIFIED] An account with the MERCHANT role but no profile now correctly triggers the Setup flow.
- **Pending State**: [VERIFIED] Verified that merchants with status `pending_business_verification` see the status banner and cannot access the "Add Item" button.
- **Approved Creation**: [VERIFIED] Once status is changed to `active` via admin, the "Add Item" button appears and listings are successfully saved to the marketplace.
- **Build Status**: [SUCCESS] Successfully compiled and verified the Android app.

## Deployment Instructions
To activate these repairs on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
