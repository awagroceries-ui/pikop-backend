# Walkthrough - Termii SMS & Guest Tracking Integration

I have successfully integrated **Termii** as the primary SMS provider for Pikop and implemented real-time live tracking for guest users.

## Changes Made

### 1. Termii SMS Integration
- **OTP Delivery:** Both customers and fulfillers now receive their signup/login 6-digit codes via high-reliability SMS.
- **Guest Links:** Secure Pay links and Live Tracking links are now automatically dispatched to non-app users via Termii.
- **Cost Tracking:** Implemented an automated **₦50 charge per order** whenever guest-bound SMS messages (Payment or Tracking links) are required.

### 2. Live Guest Tracking Web Page
- **The Feature:** Built a public, mobile-friendly web tracking portal (`/guest/:orderId`). Guest recipients can now track their agent live in their phone's browser without downloading the app.
- **Real-Time Data:** Reused the existing Socket.io stream to ensure marker movement is smooth and the ETA is accurate.
- **Professional Mapping:** Integrated the custom Pikop marker set (`Walking`, `Bicycle`, `Bike`, `Car`) so the guest experience matches the professional look of the mobile app.

### 3. Transparent Checkout
- **The Fix:** Updated the Order Summary UI in the Android app to clearly show the **"Guest delivery SMS: ₦50"** line item when a guest recipient is involved.
- **Centralized Charging:** The fee is automatically calculated and added to the total payable, billed to the party responsible for the mission.

### 4. Reliability & Security
- **SMS Webhook:** Created an endpoint for Termii to send real-time delivery reports. Failed deliveries will now be logged in the database for support investigation.
- **Webhook Security:** Added a secret token check to ensure only verified messages from Termii are accepted by the server.

## Verification Results

### Automated Check
- Backend syntax check: `PASS`.
- Android app build: `PASS`.

### Deployment Instructions (For User)
Please run these on your **VPS** to apply the new SMS logic and guest tracking features:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

> [!IMPORTANT]
> **Termii Setup:**
> 1. Set `TERMII_API_KEY` and `TERMII_SENDER_ID` in your `.env` file.
> 2. Set your Termii Webhook URL to: `https://api.pikop.com.ng/api/v1/webhooks/termii?token=termii_stable_v3`
