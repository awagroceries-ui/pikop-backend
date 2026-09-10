# Implementation Plan - Unified Wallet System (Fix for "Missing" Top-ups)

This plan permanently resolves the recurring issue where wallet top-ups (especially for fulfillers) do not reflect in the app balance by unifying all financial activity into a single, user-centric wallet.

## Problem Description
1.  **Wallet Fragmentation:** Fulfillers currently have two separate wallets: a `USER` wallet (for top-ups and Secure Pay) and a `FULFILLER` wallet (for mission earnings).
2.  **Role-Based Read/Write Discrepancy:** The Paystack Webhook always credits the `USER` wallet. However, when an agent is in "Fulfiller Mode," the app only shows their `FULFILLER` wallet.
3.  **Result:** Top-ups appear successful in the admin dashboard (which audit's all transactions) but are invisible to the agent because they are stored under a different owner ID.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725594000000_unify_wallet_system.js)
- **Heal Script:** A migration that automatically finds all `FULFILLER` wallets, identifies their corresponding `user_id`, and merges their balances and history into the main `USER` wallet.
- **Cleanup:** Marks the `FULFILLER` wallets as obsolete or deletes them.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- **Standardize:** Update all functions (`ensureWalletExists`, `recordEntry`, `processMissionSettlement`, `releaseEscrow`) to **always** use `owner_type = 'USER'` and the `user_id`.
- **Remove Fulfiller ID Dependency:** Even mission earnings will now be credited directly to the `USER` wallet using the `user_id` linked to that fulfiller.

#### [MODIFY] [walletController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/walletController.js)
- **Simplify `getMyWallet`**: Remove the role check. Always fetch the wallet where `owner_type = 'USER'` and `owner_id = userId`.
- **Update `requestWithdrawal`**: Ensure fulfillers can still withdraw, but they now pull from the unified balance.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Ensure all top-ups and escrow logic strictly use the unified user-based wallet.

---

### Android App
- No changes required. The app already calls `getWalletInfo()`. By fixing the backend to return a unified balance, the app will instantly "reveal" all previously "missing" funds.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **The "Heal" Test:** After running the migration, verify that a fulfiller who previously had "missing" top-ups now sees their full combined balance (Top-ups + Earnings).
2.  **Unified Flow:** Complete a top-up as a fulfiller. Verify it reflects immediately in the same balance where their earnings are shown.
3.  **Audit:** Verify the admin `Transactions` ledger still shows all activity but now correctly attributed to the single `USER` wallet.
