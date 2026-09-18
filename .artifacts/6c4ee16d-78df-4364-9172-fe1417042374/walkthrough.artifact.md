# Walkthrough - Scheduled & Future-Dated Orders

I have successfully implemented the **Scheduled Orders** feature, allowing customers to plan their deliveries and marketplace purchases up to 7 days in advance.

## Changes Made

### 🗓️ 1. Scheduling UI at Checkout
- **Unified Selector**: Added a "Deliver Now" vs "Schedule for Later" toggle to both the standard Dispatch and Marketplace checkout screens.
- **Native Pickers**: Integrated Android's native Date and Time pickers for a seamless scheduling experience.
- **Visual Progress**: The selected time is clearly displayed with a summary of how scheduled missions work (30-minute lead time).

### 🚀 2. Smart Activation Job (Backend)
- **Pre-emptive Dispatch**: Updated the `scheduledOrderJob.js` background worker. Instead of waiting for the exact scheduled time, it now activates missions **30 minutes prior** to the requested time. This ensures a driver is found and arriving exactly when the customer needs them.
- **Timezone Awareness**: All calculations are performed in the Africa/Lagos (WAT) timezone to ensure accuracy for local operations.

### 🛡️ 3. Multi-Layer Validation
- **Merchant Operating Hours**: For Food and Marketplace orders, the system now validates the scheduled time against the Merchant's set operating hours. Customers are blocked from scheduling when a store is closed.
- **7-Day Advance Limit**: Scheduling is capped at one week in the future to maintain operational predictability.
- **Security Windows**: Scheduled times outside the 6 AM - 6 PM daylight window are logged for "Vehicle-Only" dispatch, adhering to our established safety protocols.

### 📜 4. Reliable API Contracts
- **DTO Updates**: Updated `ApiService.kt` to include `scheduled_at` in all relevant order requests and exposed `operating_hours` in discovery results for real-time client-side validation.

## Verification Results
- **Scheduled Activation**: [VERIFIED] Confirmed missions move from `SCHEDULED` to `SEARCHING` exactly 30 minutes before their target time.
- **Merchant Gating**: [VERIFIED] Successfully blocked a scheduled meal order for a time when the kitchen was closed.
- **Immediate Refund**: [VERIFIED] Verified that cancelling a `SCHEDULED` order before it activates triggers an automatic refund to the user's wallet.
- **Build Status**: [SUCCESS] Successfully compiled and verified the Android app.

## Deployment Instructions
To activate the scheduling engine on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
