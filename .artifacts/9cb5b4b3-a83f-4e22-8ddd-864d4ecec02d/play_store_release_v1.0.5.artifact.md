# 📦 Google Play Store Release Build: Pikop v1.0.5 (Version Code 8)

**Date**: September 24, 2026
**App Version**: `v1.0.5`
**Version Code**: `8`
**Target SDK**: `36` (Android 15)
**Package Name**: `com.ng.pikop`

---

## 🎯 What's New in Version 1.0.5

1. **Merchant Setup Bank Verification & Paystack Integration**:
   - Paystack bank dropdown selector (`getBanks()`) with search filtering.
   - 10-digit account number auto-resolution (`resolveAccount()`) displaying verified account holder name.
   - Saves verified bank details (`bank_code`, `account_name`) for automatic merchant payouts.

2. **Fulfiller/Agent Onboarding Enhancements**:
   - Cascading **Operating State** and **Operating City** dropdown selectors supporting 16+ Nigeria States (`Lagos`, `Rivers`, `FCT (Abuja)`, `Oyo`, `Kano`, `Delta`, `Edo`, `Anambra`, `Enugu`, `Kaduna`, `Ogun`, `Akwa Ibom`, `Abia`, `Cross River`, `Imo`, `Plateau`, etc.).
   - Material 3 `DatePickerDialog` for Date of Birth (`dateOfBirth`).
   - `ExposedDropdownMenuBox` for Gender selection (`Male`, `Female`, `Other`).

3. **Merchant Store Operating Hours**:
   - Interactive **Store Operating Hours** settings card in **Merchant Portal -> Settings** (Opening and Closing Time selectors).
   - Validates store open/closed states for customer orders and storefront listings (`Opens at HH:MM`).

4. **Wallet System & Account Self-Deletion**:
   - Differentiates between Available Balance and Pending Escrow Balance during account deletion checks to prevent confusing `₦0.00` error blocks.
   - Permanent **Withdraw** button on Wallet Screen for all user roles with interactive withdrawal payout dialog.

5. **Retrofit Type Safety & Stability**:
   - Replaced wildcard `@Body request: Map<String, Any>` endpoints with strongly-typed request models (`CreateProductRequest`, `CreateMenuItemRequest`, `CreateMerchantCouponRequest`), eliminating Retrofit wildcard parameter exceptions.

---

## 🚀 Release Artifact File Paths

> [!IMPORTANT]
> Upload `app-release.aab` to your Google Play Console under **Production** or **Open Testing** track.

- **Google Play App Bundle (.aab)**:
  `app/build/outputs/bundle/release/app-release.aab`

- **Signed Release APK (.apk)**:
  `app/build/outputs/apk/release/app-release.apk`

- **R8 / Proguard De-obfuscation Mapping File**:
  `app/build/outputs/mapping/release/mapping.txt`

---

## 🖥️ Server Deployment Quick Check

Ensure your production VPS server (`api.pikop.com.ng` / `root@srv1932412`) is up to date:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
pm2 logs pikop-v3 --lines 30
```
