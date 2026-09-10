# Walkthrough - COD Platform Fee Consistency & Checkout Fix

I have resolved the issues with the COD platform fee application and corrected the checkout summary display to handle different user roles (Buyer vs. Seller) correctly.

## Changes Made

### 1. Backend: Corrected Role-Based Billing
- **Enforced "Buyer Pays Fee":** Updated `getQuote` in `orderController.js` to ensure the platform fee is always attributed to the **PAYER** (Buyer), regardless of who initiates the mission.
- **Fixed Upfront Total:**
    - If the **Buyer** initiates the mission, they pay the full amount (Item + Delivery + Fee + SMS) upfront.
    - If the **Seller** initiates, they now only pay the **Delivery Fee + SMS Charge** upfront. The system correctly excludes the item price and protection fee from the seller's initial payment.
- **Rounding Logic:** Per your preference, the original rounding logic (`Math.floor`) was maintained, allowing for a ₦0 fee on extremely low-value items if applicable.

### 2. Android: Transparent Order Summary
- **Split Billing View:** Updated `OrderQuoteScreen.kt` to show a clear breakdown of the costs.
- **Role Awareness:**
    - For **Buyers**: The summary confirms they are paying for the item and the protection fee.
    - For **Sellers**: The summary explicitly states: *"You are only paying for delivery now. The Recipient will pay ₦[Total] for the item."*
- **UI Hardening:** Refactored the `SummaryLine` component to support bold weights for subtotal rows, improving scanability.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these logic updates to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

## 📋 Testing Scenarios
1. **As a Seller:** Start a Secure Pay request for a ₦5,000 item. Verify that your "Total Payable" only includes the delivery cost (e.g. ₦800).
2. **As a Buyer:** Start a Secure Pay request. Verify that your "Total Payable" includes the ₦5,000 item + ₦500 platform fee + delivery.
3. **Data Accuracy:** Verify the "Subtotal (Recipient pays)" line appears correctly when you are acting as the seller.
