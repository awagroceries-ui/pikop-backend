# Walkthrough - Wallet Balance & History Sync Fixes

I have fixed the critical backend and frontend issues preventing wallet balances and mission history from reflecting correctly.

## Changes Made

### 1. Backend: Fixed SQL Query Logic
- **The Cause:** I discovered that the `INSERT INTO orders` queries in `orderController.js` and `paymentController.js` had incorrect parameter indexing and were missing columns. This caused order creation to either fail silently or save incomplete data, leading to empty histories and zero earnings.
- **The Fix:**
    - Corrected the 34+ parameter mapping for both the Webhook and Manual order creation paths.
    - Explicitly cast `coupon_id` to UUID to prevent PostgreSQL data-type inference errors.
    - Ensured `original_delivery_fee` and `original_total_fare` are always saved, guaranteeing that fulfillers get their 75% payout based on pre-discount prices.

### 2. Android App: Instant UI Refresh
- **Wallet Screen:** Added a `LifecycleEventObserver`. The wallet balance and transaction list now automatically refresh every time you return to the screen (e.g., after a payment or from another tab).
- **Fulfiller History:** Similarly, the "Missions" history now refreshes on focus, ensuring that a newly completed delivery appears in the list immediately.
- **Improved Code Robustness:** Fixed missing imports and scope handling in the Kotlin feature modules.

### 3. Fulfiller Earnings Calculation
- Updated the `getFulfillerOrders` query to perform real-time calculation of earnings: `ROUND(COALESCE(original_delivery_fee, delivery_fee) * 0.75, 2)`. This ensures the "Lifetime Earnings" card on the mobile app is always accurate.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these critical logic fixes to your **VPS**:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **Wallet Top-up:** Add money to your wallet. When the browser redirects you back to the app, the balance should update **immediately** without a restart.
2. **Complete Mission:** As a fulfiller, complete a mission. Go to the "Missions" tab; it should now appear with the correct 75% earning value.
3. **Free Mission:** Test a mission with a 100% discount. Verify that the fulfiller still receives their correct payout based on the original price.
