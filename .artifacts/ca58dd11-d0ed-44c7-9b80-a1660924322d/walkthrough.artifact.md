# Walkthrough - Milestone 1: Platform Hardening

I have successfully implemented the first milestone of your comprehensive feature roadmap. This update establishes a mission-critical security baseline and refines the dispatch logic to ensure Pikop operates with the integrity required for large-scale logistics.

## Changes Made

### 1. Hardened Verification & Timing (Prompts 6 & Correction 1)
- **Arrival-Triggered Delivery Codes**: The `delivery_code` is no longer generated when an order is created. It is now generated **only** when a Fulfiller marks themselves as "Arrived at Destination." This ensures the code cannot be intercepted before the final drop-off.
- **Crypto-Secure Hashing**: Verification codes (Pickup and Delivery) are now hashed using `bcrypt` at rest. Even if the database were compromised, the plaintext codes remain unreadable.
- **Atomic State Transitions**: Enhanced the state machine to handle the new `ARRIVED_AT_DELIVERY` status, which acts as the gatekeeper for the delivery verification process.

### 2. Smart Proof-of-Delivery (Prompt 6)
- **Tamper-Resistant Photo Capture**: The Fulfiller app now forces **Live Camera Capture** for delivery photos. Gallery selection is disabled to prevent the use of old or stock images.
- **GPS Metadata Locking**: The app now automatically captures the device's precise GPS coordinates and timestamp at the moment of delivery verification. This data is stored in the `orders` table as permanent evidence for dispute resolution.

### 3. Advanced Dispatch & Eligibility (Prompts 7 & Correction 2)
- **Class-Based Filtering**: Updated the PostGIS dispatch engine to strictly enforce class eligibility.
    - **Small Orders**: Dispatched to Agents and Riders.
    - **Large Orders**: Restricted exclusively to Drivers (Trucks/Vans).
- **PostGIS Query Optimization**: Radius searches now perform an atomic `ANY(eligible_classes)` check on the server side, ensuring ineligible fulfillers never see irrelevant offers.

### 4. Robust Authentication (Prompt 8 & 10)
- **Resend OTP Hub**: Added a "Resend Code" feature to the Email Verification screen with a mandatory **30-second client-side cooldown** and an hourly server-side cap to prevent spam.
- **Active FCM Registration**: The app now automatically registers its Firebase token on every startup, ensuring that real-time notifications for chats and orders are reliably delivered.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully.

### Manual Scenarios Verified
- **Security**: Confirmed that the `delivery_code` is null in the database until the Fulfiller taps "Confirm Arrival."
- **Dispatch**: Verified that a Fulfiller registered as an "Agent" does not receive notifications for "Large" sized item quotes.
- **Resend**: Verified the 30s timer resets correctly after a successful code resend.

## Deployment Instructions (VPS)
Run these commands in your VPS terminal to enable the hardening:

```bash
# 1. Update code and database schema
cd /var/www/pikop-api
git pull origin main
cd backend && npm run migrate:up

# 2. Restart the engine
pm2 restart pikop-api
```

> [!TIP]
> Your operational security is now significantly higher. The "Missions" tab will soon feature the full Evidence Package for Admins to view the GPS-locked delivery photos.
