# 📋 Implementation Plan: Account Deletion UX and Wallet Withdraw Button Visibility

Fix the user experience dead-end where users with a zero available balance but non-zero pending balance are unable to delete their account and are confused by missing withdrawal options.

---

## 🔍 Root Cause Analysis

1. **Confusing Account Deletion Error Message**:
   - In `authController.js` (`deleteAccount`), if a user has a `balance` of `0.00` but a `pending_balance` of `1500.00`, the deletion is blocked. However, the error message only formats the `balance` variable:
     `You have a non-zero wallet balance (₦0.00). Please withdraw your funds...`
   - This causes extreme user confusion as they are told they have funds but see `₦0.00`.

2. **Hidden Withdraw Button**:
   - In `WalletScreen.kt`, the "Withdraw" button is entirely hidden if `balance <= 0`. Users looking to withdraw (even if confused by the pending balance) complain that the wallet has "no existing withdraw option", leading them to believe the app is broken.

---

## 🛠️ Proposed Changes

### Backend API Controller

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Update the `deleteAccount` wallet check error message to differentiate between available balance and pending balance.
- If only pending balance exists, instruct the user to "wait for pending funds to clear or be refunded" instead of telling them to "withdraw".

### Android Mobile App (`:app`)

#### [MODIFY] [WalletScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/wallet/WalletScreen.kt)
- Make the "Withdraw" button **always visible**, but disable it (`enabled = false`) if `balance <= 0`. This assures the user that the withdrawal feature exists in the platform.

---

## 🧪 Verification Plan

1. Update backend message and restart VPS.
2. Update `WalletScreen.kt`, rebuild APK, and deploy to device.
3. Observe that a zero-balance user sees a disabled "Withdraw" button.
4. Stage, commit, and push to GitHub `main`.