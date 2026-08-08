# Walkthrough - Didit KYC Integration

I have successfully integrated **Didit KYC** into the Pikop Fulfiller onboarding process. This replaces the manual ID and selfie verification with an automated, professional identity suite, including passive liveness and face matching.

## Changes Made

### 1. Automated Identity Verification (Backend & Didit)
- **Didit Service**: Created `diditService.js` to handle secure session creation and signed webhook verification using HMAC-SHA256.
- **Workflow Configuration**: Integrated the "Free KYC" bundle (`f718c93d-e9d2-432b-b964-7ebf702eceb8`) which provides 500 free verifications per month.
- **Webhook Security**: Implemented the required canonicalization (sorting keys and shortening floats) to safely verify Didit's `X-Signature-V2` header.

### 2. Streamlined Fulfiller Onboarding (Android)
- **Native SDK Integration**: Added the `me.didit:didit-sdk` to the Android app, providing a smooth, non-WebView experience for identity capture.
- **Refactored KYC Screen**: Replaced the manual "ID Card" upload with a high-impact **"Identity Verification"** card.
    - Tapping the card launches Didit's automated flow.
    - The screen dynamically updates the status (Pending, Verified, Needs Review) based on backend updates.
- **Selective Manual Uploads**: Maintained the manual upload system for vehicle-specific operational documents (License, Insurance), keeping them separate from the identity check.

### 3. Unified Admin Hub (Dashboard)
- **Consolidated KYC Queue**: Redesigned the Admin KYC queue to show a unified view of each Fulfiller's status.
- **Didit Status Indicators**: Operators can now see Didit's real-time decision directly in the queue.
- **Operational Deep-Links**: Added a "View on Didit" link for any sessions flagged as `needs_review`, allowing Ops staff to inspect Didit's detailed analysis before overriding.
- **Final Authorization Gate**: The "Final Authorize" button is now intelligently disabled until **both** Didit approves the identity **and** all vehicle documents are approved by your team.

### 4. Database Infrastructure
- **Fulfiller Extension**: Added `didit_session_id`, `didit_verification_status`, and `didit_verified_at` to track the identity lifecycle.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully with the new Didit SDK dependency.

### Logic Verification
- Verified that the backend correctly maps Didit's `Approved` status to Pikop's `approved` status.
- Confirmed that the "Final Authorize" gate properly enforces the multi-step approval requirement.

## Deployment Instructions (VPS)

Run these commands on your VPS to go live with Automated KYC:

```bash
# 1. Update code and database schema
cd /var/www/pikop-api
git pull origin main
cd backend && npm install
npm run migrate:up

# 2. Add Didit Secrets to .env
# nano /var/www/pikop-api/backend/.env
# Add these lines:
DIDIT_API_KEY=WfiDIYa8EkP6DVRx0EQmy61Vz7f8a-PFa_-RZF5UlXY
DIDIT_WEBHOOK_SECRET=your_actual_webhook_signing_secret_here

# 3. Restart the engine
pm2 restart pikop-api
```

> [!IMPORTANT]
> You must register your webhook URL (`https://api.awa.name.ng/api/v1/fulfillers/kyc/webhook`) in the **Didit Console** to receive automated approval notifications!
