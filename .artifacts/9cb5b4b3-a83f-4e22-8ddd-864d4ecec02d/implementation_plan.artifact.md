# 📋 Comprehensive Implementation Plan: Merchant Setup, Agent Onboarding, Admin Media, Account Deletion & Wallet Withdrawals

Fix merchant bank setup, agent onboarding UI controls, admin media rendering, user account self-deletion wallet balance checks, and enable wallet withdrawals for all user types.

---

## 🔍 Root Cause & Requirements Summary

1. **Merchant Setup Bank Verification**:
   - `MerchantBusinessSetupScreen.kt` lacked Paystack bank selection list (`getBanks()`) and automatic account holder name resolution (`resolveAccount()`).
   - Fix: Add Paystack bank dropdown and account name resolution card in merchant setup, persisting `bank_code` and `account_name`.

2. **Agent Onboarding UI Controls**:
   - `SignupFulfillerScreen.kt` lacked native `DatePickerDialog` for Date of Birth and `ExposedDropdownMenuBox` for Gender.
   - Fix: Integrate Material 3 `DatePickerDialog` for DOB and dropdown selection for Gender (`Male`, `Female`, `Other`).

3. **Admin Dashboard Review Media Display**:
   - Image URLs in `kyc_review.ejs`, `fulfiller_detail.ejs`, and `corporate_admin.ejs` failed due to relative path formatting or file extension checks.
   - Fix: Implement `resolveMediaUrl()` helper to resolve absolute host paths, Base64 data URIs, and cloud URLs cleanly.

4. **Account Self-Deletion Wallet Balance Gating**:
   - In `authController.js` (`deleteAccount`), floating point string parsing (`bal > 0`) blocked account deletion for users whose wallet balance displayed as `₦0.00` (e.g. `0.0000001` or string noise).
   - Fix: Use threshold comparison `bal >= 0.01 || pend >= 0.01` and display exact formatted balance in error message if non-zero.

5. **Wallet Withdrawal Option for All User Types**:
   - In `walletController.js` (`requestWithdrawal`), non-fulfillers were blocked with `Only fulfillers can withdraw`.
   - In `WalletScreen.kt` (Android app), the "Withdraw" button was hidden for non-fulfillers.
   - Fix: Enable `requestWithdrawal` for all user roles (Customers, Merchants, Fulfillers) and display the "Withdraw" button & dialog for any user with `balance > 0`.

---

## 🛠️ Proposed Changes

### Android Mobile App (`:app`)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `SetupMerchantRequest` model to include `bank_code: String? = null` and `account_name: String? = null`.
- Update `WithdrawalRequest` model to accept optional bank details (`bank_name`, `account_number`, `bank_code`, `account_name`).

#### [MODIFY] [MerchantBusinessSetupScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/MerchantBusinessSetupScreen.kt)
- Integrate Paystack bank list dropdown (`getBanks()`) with search filter.
- Add account number auto-resolution trigger (`resolveAccount()`) and display verified account holder name.

#### [MODIFY] [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt)
- Add Material 3 `DatePickerDialog` for Date of Birth.
- Add `ExposedDropdownMenuBox` for Gender selection (`Male`, `Female`, `Other`).

#### [MODIFY] [WalletScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/wallet/WalletScreen.kt)
- Display **"Withdraw"** button for any user role when `balance > 0`.
- Add interactive **WithdrawalDialog** allowing users to enter amount and request payout.

---

### Backend API & Admin Dashboard Views

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Update `deleteAccount` to use threshold comparison `bal >= 0.01 || pend >= 0.01` and display exact formatted balance if non-zero.

#### [MODIFY] [walletController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/walletController.js)
- Update `requestWithdrawal` to support withdrawals for Customers, Merchants, and Fulfillers using profile or provided bank details.

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Update `setupMerchantProfile` to accept and save `bank_code` and `account_name`.

#### [MODIFY] [kyc_review.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kyc_review.ejs), [fulfiller_detail.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fulfiller_detail.ejs), [kyc_queue.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kyc_queue.ejs), [corporate_admin.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/corporate_admin.ejs)
- Ensure all media and image links resolve cleanly using `resolveMediaUrl()`.

---

## 🧪 Verification Plan

### Manual & Device Testing
1. **Account Self-Deletion Test**: Attempt account deletion on zero-balance account and verify success.
2. **Wallet Withdrawal Test**: Verify "Withdraw" button appears on `WalletScreen.kt` for positive balance, submits request, and processes withdrawal.
3. **Merchant Bank Verification**: Test Paystack bank dropdown and account name auto-resolution in Merchant setup.
4. **Agent Onboarding UI Controls**: Test Date Picker dialog and Gender dropdown in Agent signup.
5. **Admin Dashboard Media**: Verify photos and document previews on `/admin/kyc`, `/admin/kyc/:id`, and `/admin/fulfillers/:id`.
6. **Git & VPS Deployment**: Stage, commit, push to GitHub `main`, restart PM2 on VPS, and deploy APK to device.
