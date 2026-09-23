# Walkthrough - Quote Fetch Diagnostic & Debug Error Reporting

I have diagnosed the quote request payload and added server-side error diagnostics to pinpoint the quote calculation error on the live VPS server.

## Diagnostic Summary
- **Network Verification**: Re-verified HTTPS connectivity to `api.pikop.com.ng`. Network authentication and user profile endpoints return **200 OK**.
- **Payload Test**: Created a test script executing the exact order quote payload sent by the Android test device (`delivery_address`, `pickup_address`, `item_price`, `landmarks`, `coordinates`).
- **Server Response**: The live server returned `500 Internal Server Error` with `{"success":false,"message":"Unable to calculate delivery quote..."}`.

---

## Changes Made

### 🛡️ 1. Extended Error Reporting (`orderController.js`)
- Added `debug_error` and `debug_stack` to the `getQuote` exception handler response.
- When an uncaught exception occurs during quote calculation, the server now returns the precise error message and line number, allowing instant isolation of missing settings or PostGIS function calls.

### 🧪 2. Re-tested Payload
- Updated test suite (`test_quote.js`) to execute end-to-end signup, OTP verification, login, and quote calculation.

---

## Verification Results

- **Syntax Check**: [VERIFIED] `orderController.js` passed syntax checks (`node -c`).
- **Git Push**: [SUCCESS] Pushed commit `e09ab966` to `origin/main`.

---

## Required Deployment Step on VPS

Please run these commands on your VPS terminal (`root@srv1932412`) to deploy the latest quote handler updates:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

Once the VPS server restarts with the latest code, quote calculations will operate properly or display the exact diagnostic error trace.
