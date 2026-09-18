# Walkthrough - In-App Wallet Checkout

I have implemented the **Pay-from-Wallet** feature, allowing customers to use their existing Pikop balance to pay for deliveries and marketplace orders instantly.

## Changes Made

### 💰 1. Wallet Payment Engine (Backend)
- **Atomic Debit Logic**: Added `processIndividualWalletPayment` to `walletService.js`. This service uses database-level row locking (`FOR UPDATE`) to ensure that a user cannot double-spend or go below zero during a checkout transaction.
- **Order Integration**:
    - Updated `orderController.js` to support the `wallet` payment method. Wallet-paid missions are marked as **PAID** immediately upon creation and skip the browser payment step.
    - Updated `commerceController.js` to support **WALLETPAY** for marketplace and food orders.

### 📱 2. Unified Checkout UI (Android)
- **Balance Visibility**: The checkout screens (`OrderQuoteScreen` and `CommerceCheckoutScreen`) now automatically fetch the user's available wallet balance on load.
- **Dynamic Selection**: Added a "My Wallet" option to the billing method selector.
    - **Smart Gating**: The option is only selectable if the user's balance is sufficient to cover the total amount (including insurance and fees).
    - **Visual Feedback**: If the balance is too low, the wallet option shows a lock icon and is disabled to prevent failed payment attempts.
- **Zero-Friction Activation**: When paying via wallet, the app bypasses the Paystack browser entirely and activates the mission with a single click.

### 📜 3. Auditable Ledger
- Every wallet payment is recorded in the transaction history with a unique `MISSION_PAYMENT` reference, ensuring both the customer and admins have a clear paper trail of how funds were spent.

## Verification Results
- **Payment Flow**: [VERIFIED] Verified that selecting "My Wallet" correctly deducts the amount and triggers immediate mission status update to `SEARCHING`.
- **Insufficient Funds**: [VERIFIED] Verified that the "My Wallet" chip is disabled and shows the lock icon when the order total exceeds the available balance.
- **Fee Integrity**: [VERIFIED] Confirmed that insurance fees and platform commissions are correctly calculated and collected regardless of the wallet payment method.
- **Build Status**: [SUCCESS] Successfully compiled the Android app.

## Deployment Instructions
To activate wallet-based checkout on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
