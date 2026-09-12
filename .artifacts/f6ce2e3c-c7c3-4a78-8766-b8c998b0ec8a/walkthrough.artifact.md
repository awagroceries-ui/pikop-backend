# Walkthrough - Fleet Module Phase 1: Automated Payouts

I have successfully implemented the **Automated Payouts** phase of the Fleet module. Pikop fulfillers can now withdraw their earnings directly to their bank accounts with instant processing for smaller amounts.

## New Capabilities

### 1. Instant Payout Integration
- **Automated Transfers:** Integrated the **Paystack Transfer API** into the withdrawal flow.
- **Instant Processing:** Withdrawals of **₦5,000 or less** are now processed immediately by the system, bypasssing the need for manual admin approval.
- **Thresholds:** Implemented a minimum withdrawal limit of **₦1,000** to ensure cost-effective processing.

### 2. Dedicated Withdrawal Interface
- **The Screen:** Created `WithdrawalScreen.kt` in the Android app.
- **Safety First:** The screen clearly displays the linked bank account details, allowing agents to verify where their money is going before they hit "Submit."
- **Live Validation:** The "Withdraw" button only activates if the agent has a valid bank account on file and enough balance.

### 3. Automatic Recipient Registration
- **Zero Configuration:** The first time an agent requests a withdrawal, the system automatically registers them as a "Transfer Recipient" on Paystack using their profile's bank details.
- **Speed:** Future withdrawals are even faster as the recipient record is reused.

### 4. Hardened Merchant APIs
- **Ownership Verification:** Added strict checks to the Marketplace and Kitchen controllers to ensure merchants can only modify their own items.
- **Inventory Cleanup:** Implemented full "Delete" functionality for products and menu items.

## Verification Results

### Backend Integrity
- Verified the new `requestWithdrawal` logic and Paystack API mapping.
- **Result:** `PASS`.

### Android Build
- Successfully compiled the new Withdrawal UI and integrated it with the main Wallet navigation.
- **Result:** `BUILD SUCCESSFUL`.

## Deployment Instructions (VPS)
Please apply these automated payout updates to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
