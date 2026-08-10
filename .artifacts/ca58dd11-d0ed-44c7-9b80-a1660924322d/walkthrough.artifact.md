# Walkthrough - Milestone 4: Viral Growth & Public Tracking

I have successfully implemented **Milestone 4**, introducing a suite of features designed to drive organic growth through referrals and provide a premium, transparent tracking experience for recipients.

## Changes Made

### 1. Shareable Live Tracking (Web & Real-time)
- **Public Tracking Portal**: Created a lightweight, responsive web page (`/track/:token`) that allows anyone with the link to watch a delivery's progress in real-time without logging in.
- **Privacy-First Data**: The public portal automatically masks exact house addresses and sensitive item details, showing only the general pickup/delivery zones.
- **Web-Socket Sync**: Extended the real-time engine to broadcast live driver location and status updates directly to the public web portal.
- **Android Native Share**: Added a "Share Tracking" button in the app that leverages the system share sheet (WhatsApp, SMS, etc.) for instant link sharing.

### 2. Viral Growth Engine (Referrals & Rewards)
- **Automated Referral Codes**: Every new Pikop user now automatically receives a unique, 8-character referral code upon signup.
- **Incentivized Onboarding**: Users can enter a referral code during registration.
- **Conversion-Locked Rewards**: Implemented the "First Delivery Rule." Referral credits (₦500 for the Referrer, ₦300 for the Referee) are only issued after the new user completes their **first successful delivery**, protecting the platform from signup fraud.

### 3. Promotional Discount System
- **Promo Code Engine**: Built a backend system to manage campaign-based discounts (Flat NGN or Percentage-based).
- **Checkout Integration**: Added a "Promo Code" input to the order creation flow with real-time validation and instant discount calculation.
- **Usage Gating**: Codes are automatically validated against expiration dates, usage limits, and per-user redemption rules.

### 4. High-Trust Fulfiller Transparency
- **Public Profile Injection**: The public tracking link now includes the Fulfiller's professional identity card, showing their name, tier badge, and verified star rating.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully.

### Manual Scenarios Verified
- **Referral Flow**: Verified that a new user signed up with a code correctly links to the referrer.
- **Tracking**: Confirmed the public URL correctly renders the live map and driver position in a standard web browser.
- **Promo Logic**: Verified that an expired code returns a professional "Code invalid or expired" message in the app.

## Deployment Instructions (VPS)
Run these commands in your VPS terminal to enable Growth & Tracking:

```bash
# 1. Update code and apply growth schema
cd /var/www/pikop-api
git pull origin main
cd backend && npm run migrate:up

# 2. Restart the Mission Engine
pm2 restart pikop-api
```

> [!TIP]
> You can now manage your marketing campaigns by creating codes in the `promo_codes` table. Users can find their personal referral code under the **Menu** tab in the app!
