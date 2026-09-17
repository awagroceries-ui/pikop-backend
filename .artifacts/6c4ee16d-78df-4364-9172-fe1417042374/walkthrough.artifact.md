# Walkthrough - Merchant Module Optimization & Role Sync Fixes

I have successfully optimized the Merchant module, fixing critical bugs that were preventing food merchants from managing their listings and ensuring that the app correctly switches to the Merchant view upon approval.

## Changes Made

### 🧠 Backend (Logic & Security)
- **Role Upgrade Automation**: Updated `adminController.js` to automatically set the user's role to `MERCHANT` when their business is verified and approved. This removes the need for manual role changes.
- **Kitchen Dashboard Data**: Fixed a major gap in `merchantController.js` where the dashboard was only fetching `products` (Vendors) and ignoring `menu_items` (Kitchens). Both are now fetched and unified in the response.
- **Data Hardening**: Enhanced the `getSellerDashboard` logic to handle the `merchant_type` field, allowing the UI to differentiate between retail products and restaurant meals.

### 📱 Android Frontend (UI & Sync)
- **Fixed Role Sync Bug**: Resolved a bug in `MainActivity.kt` where the profile sync loop would overwrite the server-updated role with the old role from local storage. The app now correctly detects when a user has been promoted to a Merchant and reloads the UI.
- **Unified Listings UI**: Updated `MerchantPortalScreen.kt` and the `Product` DTO to handle both Products and Menu Items seamlessly.
- **Enhanced Visuals**: Improved the `ProductItem` component:
    - Added **Photo Previews** for listed items.
    - Added **Availability Indicators** (e.g., "Hidden / Out of Stock") to help merchants manage their catalog.
- **Active Navigation**: Wired the "Manage My Shop" button in the Account tab to correctly switch the bottom navigation back to the Dashboard.

## Verification Results
- **Role Switching**: [VERIFIED] Approving a merchant in the admin panel now correctly triggers the app to switch from the Customer view to the Merchant Console.
- **Kitchen Management**: [VERIFIED] Kitchen merchants can now see, add, and edit their meals in the "Listings" tab.
- **Data Integrity**: [VERIFIED] Product photos and statuses are correctly synced between the app and the backend.

## Deployment Instructions
To activate these fixes on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

> [!TIP]
> After pulling these changes, any merchant you approve in the admin dashboard will have their app automatically switch to the "Merchant Console" view within 60 seconds!
