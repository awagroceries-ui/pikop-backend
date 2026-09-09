# Walkthrough - Termii SMS Integration & Guest Tracking

I have integrated **Termii** as the official SMS provider for Pikop, enabling secure onboarding, guest payments, and a new real-time guest tracking experience.

## Changes Made

### 1. Unified Termii SMS Service
- **OTP via SMS:** Implemented `sendOtp` and `verifyOtpToken` using Termii's high-reliability OTP product. Fulfillers and Customers now receive a 6-digit verification code via SMS during signup and resend requests.
- **Guest Links:** Wired Secure Pay payment links and tracking links to send automatically via Termii SMS to recipients who don't have the app.
- **Auditing:** Created a new `sms_logs` table. Every message sent is recorded with its recipient, content, purpose, and provider reference for operational cost tracking.

### 2. Public Guest Tracking Page
- **The Feature:** Built a lightweight, browser-based tracking page (`guest_tracking.ejs`). Guest receivers can now track their delivery live without downloading the app.
- **Real-time Movement:** The web page reuses the same Socket.io data stream as the mobile app. The agent icon moves smoothly on the map as their GPS coordinates update.
- **Visual Consistency:** Exported and integrated the custom marker icons (Walking, Rider, Bike, Car) so the guest web experience matches the professional look of the Pikop app.

### 3. Automated ₦50 Guest SMS Charge
- **Charging Policy:** Every order involving a guest recipient now automatically incurs a **one-time ₦50 fee**.
- **Billing:** This fee is added to the `total_payable` and billed to the order's payer (the party responsible for the platform fee).
- **Transparency:** Added a "Guest SMS Charge: ₦50" line item to the Order Summary UI in the Android app, ensuring the payer sees exactly what they are being charged for.

### 4. Termii Webhook & Sync
- **Webhook Endpoint:** Implemented `POST /api/v1/webhooks/termii`. This endpoint is ready for configuration on the Termii dashboard to receive real-time delivery reports.
- **Reliability:** The system now automatically updates the `sms_logs` when Termii confirms a message has been successfully delivered to the carrier.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these critical schema and SMS updates to your **VPS**:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

> [!IMPORTANT]
> **Termii Configuration:**
> 1. Set `TERMII_API_KEY` in your VPS `.env` file.
> 2. Set your registered `TERMII_SENDER_ID` (default is `Pikop`).
> 3. Configure your Termii Webhook URL to: `https://api.pikop.com.ng/api/v1/webhooks/termii`
