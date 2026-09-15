# Walkthrough - Merchant Operating Hours & Daylight Dispatch Security

I have implemented two major features focused on platform predictability and personnel safety: **Merchant Operating Hours** and **Time-Restricted Dispatch**.

## Changes Made

### 🏪 1. Merchant Operating Hours
- **Configurable Schedule**: Added `operating_hours` to Merchant profiles. Merchants can now define their open/close windows for the week.
- **Closed-State Discovery**: The Android app now overlays a "CLOSED" indicator on stores that are outside their operating window.
- **Hard Checkout Block**: The commerce engine now hard-rejects orders for closed merchants with a clear message: *"Store is currently closed. Opens at [Time]."*

### 🛡️ 2. Daylight Dispatch Security (6 AM - 6 PM)
- **Automatic Category Filtering**: Between 6:00 PM and 6:00 AM (Nigeria Time), the dispatch engine automatically excludes **Foot Agents, Cyclists, and Riders** from new missions.
- **Driver-Only Night Window**: Only **Drivers** (cars/vans) remain eligible for night-time dispatch, enhancing safety for the fleet.
- **Admin Configurability**: The daylight window is fully configurable via the `settings` database table, allowing admins to adjust based on seasonal or security conditions.

### 📅 3. Next-Day Scheduling
- **Intelligent Fallback**: If a Customer requests a mission at night when no Drivers are online, the app now presents a choice: *"No drivers available right now — schedule this for tomorrow morning?"*.
- **Automated Activation**: Missions scheduled for the next day are saved as `SCHEDULED` and automatically activated at 6:00 AM WAT by a new background job (`scheduledOrderJob.js`).

### 🌍 4. Timezone Integrity
- Implemented `time.js` utility to ensure all logic across the backend uses **West Africa Time (WAT)**, regardless of the server's system clock.

## Verification Results
- **Android Build**: Successfully compiled (`:app:assembleDebug`).
- **Logic Tests**:
    - Verified Foot Agents are filtered out at night.
    - Verified Merchants show as "CLOSED" in discovery when out of hours.
    - Verified scheduling dialog triggers correctly on driver-empty routes.

## Deployment Instructions
To apply these changes and database migrations to your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
