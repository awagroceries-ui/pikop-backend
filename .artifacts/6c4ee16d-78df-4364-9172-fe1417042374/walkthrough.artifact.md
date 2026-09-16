# Walkthrough - Fleet Partner Program

I have successfully implemented the **Fleet Partner Program**, allowing organized logistics companies to join Pikop as partners, manage their own fleets of drivers, and benefit from custom commission rates and priority routing.

## Changes Made

### 💼 1. Fleet Partner Account & Onboarding
- **New Account Type**: Added `FLEET_PARTNER` to the system. This role is specifically designed for businesses that own a fleet but don't fulfill missions themselves.
- **B2B Onboarding Flow**: Created a dedicated, multi-step onboarding flow for Fleet Partners:
    - **Step 1**: Contact Person & Basic Info (`SignupFleetPartnerScreen.kt`).
    - **Step 2**: Business Details (CAC, Address, Fleet Size, vehicle types, cities) (`FleetPartnerBusinessSetupScreen.kt`).
- **Admin Verification**: All Fleet Partners enter a `PENDING_VERIFICATION` state, allowing admins to manually review business credentials before activation.

### 🔗 2. Driver-Fleet Linking
- **Invite Code System**: Verified Fleet Partners can generate a unique `invite_code` from their dashboard.
- **Linked Signup**: Individual Fulfillers (Riders/Drivers) can enter this invite code during their signup. This automatically links them to the partner via a `fleet_partner_id`.
- **Integrity**: Fleet-linked fulfillers still complete their own individual KYC (License/Identity/Vehicle) to maintain platform safety standards.

### 🚀 3. Priority Routing & Commission Overrides
- **Overflow Priority**: Implemented a 3-minute "independent-first" window. Fresh missions are first offered to independent agents; if unaccepted after 3 minutes, they are automatically broadcast to eligible Fleet Partners.
- **Custom Commissions**: Admins can now negotiate and set custom Pikop commission rates (e.g., 20% instead of 25%) for specific partners. This override is automatically applied during mission settlement for all drivers linked to that partner.

### 📊 4. Fleet Partner Dashboard
- **Management Console**: Built a dedicated dashboard for fleet owners to monitor their operations in real-time.
- **Insights**: Owners can see:
    - Aggregate mission volume (30-day view).
    - Status of all linked drivers (Online/Offline, KYC status, Ratings).
    - Direct access to their unique invite code.

## Verification Results
- **Onboarding Path**: [VERIFIED] Role selection correctly branches to Fleet Partner application.
- **Linking Logic**: [VERIFIED] Entering a valid invite code correctly sets the `fleet_partner_id` in the database.
- **Settlement Logic**: [VERIFIED] `walletService.js` correctly detects the partner override and adjusts the platform share accordingly.
- **Build Status**: [SUCCESS] Successfully compiled and verified (`:app:assembleDebug`).

## Deployment Instructions
To activate the Fleet Partner program on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
