# Walkthrough - 10% COD Platform Fee Audit & Fix

I have performed a comprehensive audit of the 10% COD platform fee across the backend and the mobile app to ensure mathematical consistency and UI transparency.

## Audit Findings

### 1. Backend Enforcement
- **Centralization:** Confirmed that the 10% fee is controlled by a single constant `ESCROW.FEE_PERCENTAGE` in `backend_v3/src/config/platform.js`.
- **Calculation:** The fee is calculated as `Math.floor(item_price * 0.10)`, ensuring it strictly applies to the item price only and not the delivery fee.
- **Enforcement:** The `total_payable` sent to Paystack correctly includes this fee when the `fee_payer` is `PAYER`.

### 2. Android Checkout Display
- **Order Summary:** In `OrderQuoteScreen.kt`, the cost breakdown correctly isolates the **"COD Item & Fees"** section, showing the 10% platform fee separately.
- **Consistency:** The calculation used for display matches exactly what is sent to the payment gateway.

### 3. Discrepancy Found & Fixed: Seller View
- **The Issue:** In the fulfiller/seller app (`ActiveOrderScreen.kt`), the "Mission Completed" screen was showing the **Gross Item Price** as the pending payout, even if the seller was the one paying the 10% platform fee.
- **The Fix:** Updated the UI logic to calculate and display the **Net Payout** (Item Price - Platform Fee) for sellers who are the designated fee payers.
- **Transparency:** Added a small explanatory note: `"(₦[Fee] platform fee deducted from total ₦[Item Price])"` to manage seller expectations correctly.

### 4. Admin Dashboard
- **Financial Board:** Confirmed that the admin's mission board and tracking detail view correctly list the `platform_fee_amount` and the `fee_payer`, matching the database record precisely.

## Verification Results

### Boundary Test Case
- **Scenario:** Item Price = ₦100.
- **Expected Fee:** ₦10 (10%).
- **Result:** Calculation produces exactly ₦10.00. No floating-point drift or ₦0 errors detected.

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please ensure your **VPS** has the latest `ActiveOrderScreen.kt` logic by pulling the changes:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

> [!NOTE]
> Since this was an audit and a minor UI fix, no database migrations were required.
