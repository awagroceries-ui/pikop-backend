# 📌 Task Checklist: Merchant Setup, Agent Onboarding, Admin Media, Account Deletion & Wallet Withdrawals

- `[/]` Task 1: Merchant Setup Bank Verification & Paystack Integration
  - `[ ]` Update `ApiService.kt` `SetupMerchantRequest` with `bank_code` and `account_name`
  - `[ ]` Update `MerchantBusinessSetupScreen.kt` with Paystack bank list dropdown and account resolution card
  - `[ ]` Update `merchantController.js` `setupMerchantProfile` to save bank details

- `[ ]` Task 2: Agent Onboarding UI Controls
  - `[ ]` Integrate Material 3 `DatePickerDialog` for Date of Birth in `SignupFulfillerScreen.kt`
  - `[ ]` Integrate `ExposedDropdownMenuBox` for Gender selection in `SignupFulfillerScreen.kt`

- `[ ]` Task 3: Admin Dashboard Review Media Display Fix
  - `[ ]` Update `kyc_review.ejs` with `resolveMediaUrl()` formatting
  - `[ ]` Update `fulfiller_detail.ejs`, `kyc_queue.ejs`, and `corporate_admin.ejs` to resolve absolute media URLs

- `[ ]` Task 4: Account Self-Deletion & Wallet Withdrawals
  - `[ ]` Update `authController.js` `deleteAccount` to use threshold comparison `bal >= 0.01 || pend >= 0.01`
  - `[ ]` Update `walletController.js` `requestWithdrawal` to support all user roles (Customers, Merchants, Fulfillers)
  - `[ ]` Update `WalletScreen.kt` to show "Withdraw" button for all users with `balance > 0` and add `WithdrawalDialog`

- `[ ]` Task 5: Build, Device Deployment & Git Automation
  - `[ ]` Build debug APK (`app:assembleDebug`) and deploy to connected device (`192.168.1.2:42447`)
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
