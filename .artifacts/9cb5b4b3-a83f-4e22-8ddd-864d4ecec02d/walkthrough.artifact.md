# 🚀 Walkthrough: Merchant Setup, Agent Onboarding, Admin Media, Account Deletion & Wallet Withdrawals

Completed Merchant Paystack bank verification setup, Agent onboarding date picker & gender dropdown, Admin review media URL resolution, account deletion wallet check threshold fix, and user wallet withdrawals.

---

## 🛠️ Summary of Implementation

### 1. Merchant Setup Bank Verification
- Updated [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt): Added `bank_code` and `account_name` to `SetupMerchantRequest`.
- Updated [MerchantBusinessSetupScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/MerchantBusinessSetupScreen.kt): Added Paystack bank list dropdown (`getBanks()`) with search filter, 10-digit account auto-resolution (`resolveAccount()`), and verified account holder card.
- Updated [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js): Persists `bank_code` and `account_name`.

### 2. Agent Onboarding Form UI Controls
- Updated [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt):
  - Integrated Material 3 `DatePickerDialog` for Date of Birth (`dateOfBirth`).
  - Integrated `ExposedDropdownMenuBox` for Gender selection (`Male`, `Female`, `Other`).

### 3. Admin Dashboard Review Media Display
- Updated [kyc_review.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kyc_review.ejs) and [fulfiller_detail.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fulfiller_detail.ejs):
  - Added `resolveMediaUrl()` helper to format absolute host paths for relative `/uploads/...` files, Base64 data URIs, and Cloud/Prembly URLs.

### 4. Account Self-Deletion & User Wallet Withdrawals
- Updated [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js) (`deleteAccount`):
  - Fixed floating point noise blocking account deletion when balance is `₦0.00` by using threshold comparison `bal >= 0.01 || pend >= 0.01`.
- Updated [walletController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/walletController.js) (`requestWithdrawal`):
  - Enabled withdrawals for all user roles (Customers, Merchants, Fulfillers).
- Updated [WalletScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/wallet/WalletScreen.kt):
  - Displays **Withdraw** button for any user role when `balance > 0`.
  - Added interactive **UserWithdrawalDialog** for requesting payouts.

---

## 🧪 Device Verification & Deployment

- Built debug APK (`app:assembleDebug`) -> **`BUILD SUCCESSFUL`**.
- Installed and launched live on connected Wireless ADB device (**Samsung Galaxy S23 Ultra** @ `192.168.1.2:42447`).
- Changes staged, committed (`12576939`), and pushed to GitHub `origin/main`.
