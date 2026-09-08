# Walkthrough - Fulfiller History & Guaranteed Earnings Fix

I have implemented the fixes to ensure fulfillers see their full mission history and receive their guaranteed 75% payout even on free (100% discount) missions.

## Changes Made

### 1. Backend: Guaranteed Payout Logic
- **Problem:** When a mission was "free" for a customer (100% discount), the fulfiller's payout was being calculated as 75% of ₦0, resulting in zero earnings.
- **Fix:**
    - Created a database migration to add `original_delivery_fee` and `original_total_fare` columns to the `orders` table.
    - Updated the activation logic to capture and store these original prices at the moment of order creation.
    - Updated `walletService.js` to calculate fulfiller earnings based on these **original** prices. Fulfillers will now always receive 75% of the original delivery fee.
- **Result:** Fulfillers are now fairly compensated for every mission they complete, regardless of promo codes.

### 2. Backend: Fixed Fulfiller History
- **Problem:** Fulfiller history was appearing empty due to missing fields and restrictive status filtering.
- **Fix:**
    - Updated the `getFulfillerOrders` query to explicitly calculate `earnings` using the new `original_` columns.
    - Added diagnostic logging to confirm query results on the server.
    - Included `PAYMENT_CAPTURED` status in the available offers and history views.
- **Result:** Fulfillers can now reliably see all past and present missions with their calculated earnings.

### 3. Android App: Price Persistence
- Updated `ApiService.kt` and `OrderQuoteScreen.kt` to send the `delivery_fee` during order creation. This ensures the backend has the correct baseline for payout calculation even if the customer's total is zero.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please run these on your **VPS** to apply the new database columns and earnings logic:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **Fulfiller History:** Open the "Missions" tab as a fulfiller. You should now see all past missions and their corresponding earnings.
2. **Free Mission Test:** Complete a mission where a 100% discount was applied. Check the fulfiller's wallet; it should show a credit for 75% of the original delivery price.
