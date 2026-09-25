# 📦 Google Play Store Release Build: Pikop v1.0.6 (Version Code 9)

**Date**: September 25, 2026
**App Version**: `v1.0.6`
**Version Code**: `9`
**Target SDK**: `36` (Android 15)
**Package Name**: `com.ng.pikop`

---

## 🎯 What's New in Version 1.0.6 (Version Code 9)

1. **Updated Platform Fees & Commissions**:
   - Cash on Delivery (COD) / Escrow Platform Fee set to **5%**.
   - Dispatch Commission set to **20%** (Agents keep **80%** of delivery fares).
   - Unified Marketplace Commission set to **5% across all categories** (Food, Groceries, Shop).

2. **Merchant Setup Bank Verification & Paystack Integration**:
   - Paystack bank dropdown selector (`getBanks()`) with search filtering.
   - 10-digit account number auto-resolution (`resolveAccount()`) displaying verified account holder name.
   - Saves verified bank details (`bank_code`, `account_name`) for automatic merchant payouts.

3. **Fulfiller/Agent Onboarding Enhancements**:
   - Cascading **Operating State** and **Operating City** dropdown selectors supporting 16+ Nigeria States (`Lagos`, `Rivers`, `FCT (Abuja)`, `Oyo`, `Kano`, `Delta`, `Edo`, `Anambra`, `Enugu`, `Kaduna`, `Ogun`, `Akwa Ibom`, `Abia`, `Cross River`, `Imo`, `Plateau`, etc.).
   - Material 3 `DatePickerDialog` for Date of Birth (`dateOfBirth`).
   - `ExposedDropdownMenuBox` for Gender selection (`Male`, `Female`, `Other`).

4. **Merchant Store Operating Hours**:
   - Interactive **Store Operating Hours** settings card in **Merchant Portal -> Settings** (Opening and Closing Time selectors).
   - Validates store open/closed states for customer orders and storefront listings (`Opens at HH:MM`).

5. **Wallet System & Account Self-Deletion**:
   - Differentiates between Available Balance and Pending Escrow Balance during account deletion checks to prevent confusing `₦0.00` error blocks.
   - Permanent **Withdraw** button on Wallet Screen for all user roles with interactive withdrawal payout dialog.

---

## 🚀 Release Artifact File Paths

> [!IMPORTANT]
> Upload `app-release.aab` (`versionCode = 9`) to your Google Play Console under **Production** or **Testing** track.

- **Google Play App Bundle (.aab)**:
  `app/build/outputs/bundle/release/app-release.aab`

- **Signed Release APK (.apk)**:
  `app/build/outputs/apk/release/app-release.apk`

- **R8 / Proguard De-obfuscation Mapping File**:
  `app/build/outputs/mapping/release/mapping.txt`
