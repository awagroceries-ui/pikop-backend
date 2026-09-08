# Walkthrough - Deep Diagnostic & Emergency Fixes

I have added deep diagnostic logging to the backend and corrected several potential causes for the HTTP 500 and Paystack checkout issues.

## Changes Made

### 1. Backend: Deep Diagnostic Logging
- **Places Controller:** Now logs the exact Google API URL, parameters, and full status code. This will tell us if Google is rejecting the API key or if the query is malformed.
- **Payment Controller:** Logs the successful initialization of Paystack and the exactly requested channels.
- **Order Controller:** Wrap `verifyDelivery` in a verbose logger that captures the full stack trace and returns the error message to the Android app.

### 2. "Complete Mission" (500 Error) Fixes
- **Interval Syntax:** Changed the PostgreSQL interval casting to a more robust format: `+ ($1 || ' hours')::interval`.
- **Field Selection:** Verified all necessary fields (`user_id`, `item_price`, `escrow_status`) are selected in the initial query.
- **Data Casting:** Used `parseFloat` for item price checks to ensure consistency between Postgres strings and JS numbers.

### 3. Paystack Payment Channels
- **Prioritization:** Moved `bank_transfer` and `bank` to the beginning of the `channels` array.
- **Consistency:** Applied the same `channels` array to **CoD** and **Wallet Top-up** initializations.

### 4. Android: Verbose Error UI
- Updated `ActiveOrderScreen.kt` to parse and display the specific `message` returned by the backend on 500 errors.
- **Result:** Instead of just "HTTP 500", the app will now show "Process Failure: Internal Error: <Database/Logic Error>".

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please run these on your **VPS** to apply the diagnostic tools and fixes:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

### Manual Verification Steps (Diagnostic)
1. **Complete Mission:** Use code `8888`. If it fails, **please screenshot the exact error message** shown in the Toast.
2. **Checkout:** Request an order. If Bank Transfer is missing, run `pm2 logs pikop-v3` and check for the `[Paystack]` initialization log.
3. **Address Search:** Search for "Lagos". If empty, run `pm2 logs pikop-v3` to see the `[Places]` status and URL.
