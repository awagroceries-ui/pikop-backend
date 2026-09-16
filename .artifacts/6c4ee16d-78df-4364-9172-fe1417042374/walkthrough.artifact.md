# Walkthrough - Removal of Legacy Merchant Entry Points

I have successfully decommissioned the legacy merchant registration paths that pre-dated the formal KYC/KYB onboarding. This ensures that every Merchant on the platform has undergone proper business verification.

## Changes Made

### 📱 Android Frontend (Cleanup)
- **Settings Unification**: Removed the "Join as a Merchant" tab from the `AccountScreen`.
- **Enforced Verification Flow**: Repurposed the "Merchant Portal" button. If a user doesn't have a merchant profile, it now correctly redirects them to the multi-step `Business Verification` flow (Stage 2 onboarding) instead of the old, unverified registration form.
- **Dead Code Removal**: Deleted `MerchantRegistrationScreen.kt` and its associated routes in `MainActivity.kt`.
- **API Hardening**: Removed legacy `registerVendor` and `registerKitchen` methods and their DTOs from `ApiService.kt`.

### 🛠️ Backend (Security & Consistency)
- **Endpoint Decommissioning**: Deleted the old registration endpoints in `marketplaceController.js` and `kitchenController.js`.
- **Route Removal**: Updated `marketplaceRoutes.js` and `kitchenRoutes.js` to remove the legacy registration paths.
- **Verification Integrity**: Confirmed that the only remaining path to merchant status is the proper role-selection flow which mandates business verification (CAC/NAFDAC).

## Auditing & Retroactive Review

> [!WARNING]
> **Action Required: Manual Account Audit**
> I have identified that legacy unverified merchants are stored with a status of `'pending'`.
> 1. Open the **Admin Dashboard** -> **Partners** -> **Marketplace**.
> 2. Look for any stores in the **Verification Queue** with the status **PENDING** (lowercase).
> 3. These accounts likely bypassed the new KYB flow and should be retroactively requested to complete verification or be suspended.

## Verification Results
- **Security Audit**: All bypass endpoints now return 404 (Not Found).
- **UI Integrity**: Confirmed no "Join as Merchant" links remain in Customer or Fulfiller menus.
- **Android Build**: Successfully compiled with Gradle (`:app:assembleDebug`).

## Deployment Instructions
To apply these changes to your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
