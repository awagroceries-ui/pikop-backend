# Walkthrough - Milestone 2: Fulfiller 2.0 & Advanced Onboarding

I have successfully implemented the second milestone, transforming the fulfiller experience with tailored onboarding flows, mandatory live identity capture, and professional public profiles.

## Changes Made

### 1. Tailored Fulfiller Onboarding (Android & Backend)
- **Branching Flow**: Re-engineered the onboarding process to branch based on the Fulfiller's class.
    - **Agents**: Now follow a streamlined path that skips vehicle-specific details and documents.
    - **Riders & Drivers**: Are now required to provide vehicle registration numbers, make/model, and specific licenses (Riders/Drivers License).
- **Mobility Selection**: Agents now select their `mobility_type` (on_foot, public_transit, or bicycle). Selecting **Bicycle** automatically grants a 50% radius boost in the dispatch engine to reach more customers.

### 2. Mandatory Live Profile Photo (Android Security)
- **Tamper-Resistant Capture**: Implemented a mandatory "Face Capture" step.
- **Hardware Lockdown**: The app now forces **Live Camera Capture** for profile photos. Gallery uploads are strictly blocked to ensure that every Fulfiller's profile photo is authentic and taken during the application process.

### 3. Fulfiller Public Profile Card (User App)
- **Identity Verification**: Users now see a formalized **Fulfiller Public Profile Card** as soon as they are matched with a driver.
- **High-Impact UI**: The card includes the Fulfiller's **Live Profile Photo**, name, **Tier Badge** (Bronze/Silver/Gold), and verified **Vehicle Plate Number** (for Riders/Drivers).
- **Safety Transparency**: This profile is also injected into the tracking screen, providing customers with consistent visibility of who is handling their delivery.

### 4. Advanced Dispatch Engine (Backend)
- **Cyclist radius multiplier**: Updated the PostGIS dispatch logic to support a radius multiplier. Cyclists now appear in search results at the same priority as motorized Riders.
- **Smart Offer Mapping**: Offers are now automatically filtered by size-tier eligibility (Small, Medium, Large) before being broadcast to fulfillers.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully.

### Manual Verification
- **Photo Locking**: Confirmed that the "Capture Face" button launches the system camera and cannot be bypassed via the gallery.
- **Branching Logic**: Verified that an Agent-class applicant is never prompted for a vehicle registration number.
- **Profile Display**: Verified that the Plate Number is automatically omitted from the Profile Card when the Fulfiller is an "Agent," maintaining a clean UI layout.

## Deployment Instructions (VPS)
Run these commands in your VPS terminal to enable Fulfiller 2.0:

```bash
# 1. Update code and database schema
cd /var/www/pikop-api
git pull origin main
cd backend && npm run migrate:up

# 2. Restart the engine
pm2 restart pikop-api
```

> [!TIP]
> Your Fulfillers can now progress through a much more professional onboarding experience. The **Bicycle** mobility option is a game-changer for urban Agents, allowing them to cover much more ground.
