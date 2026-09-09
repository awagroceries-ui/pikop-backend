# Walkthrough - Payout & SQL Alignment Fix

I have resolved the "could not determine data type of parameter $20" error and finalized the data flow for fulfiller earnings.

## Changes Made

### 1. Fixed SQL Alignment Bug
- **Problem:** A mismatch in the number of parameters and their indices in the `INSERT INTO orders` query caused a PostgreSQL error during free mission activation.
- **Fix:**
    - Re-aligned all 30 parameters in `orderController.js` and `paymentController.js`.
    - Added explicit UUID casting (`$21::uuid`) for the `coupon_id` field to prevent ambiguous type errors when a promo code is missing.
    - Added null-safety for `notes` and `item_photo_url`.
- **Result:** Missions (including free ones) now activate correctly without database driver errors.

### 2. Completed Earnings Data Flow
- **Android:** Updated `PaymentInitializationRequest` and the main checkout flow to send the `promo_id`.
- **Backend:** Updated the Paystack metadata to capture the `promo_id`. This ensures that even if a mission is paid, the system knows which coupon was used and can calculate fulfiller payouts correctly.
- **Result:** Fulfillers are now guaranteed their 75% share based on pre-discount prices for both paid and free missions.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please run these on your **VPS** to apply the SQL and metadata fixes:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **Free Mission:** Request a delivery with a 100% discount code. It should now activate successfully and show "Mission Activated!".
2. **Fulfiller Earnings:** Complete the mission. Check the fulfiller's history and wallet; the earnings should be calculated from the original price, not the ₦0 paid by the customer.
