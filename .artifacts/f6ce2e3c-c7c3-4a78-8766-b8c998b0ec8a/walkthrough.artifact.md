# Walkthrough - Critical Dispatch Fix (20km State Lock)

I have implemented the critical dispatch fixes to ensure missions are only matched to fulfillers within the same state and within a strictly enforced 20km radius.

## Changes Made

### 1. Hard State-Level Filtering
- **The Problem:** Mission requests were "leaking" across states (e.g., Port Harcourt orders appearing to agents in Lagos).
- **The Fix:**
    - Added `pickup_state` to all orders/quotes and `current_state` to all fulfiller profiles.
    - Updated the dispatch engine and the `getAvailableOffers` API to enforce a **hard equality match** between the order's pickup state and the fulfiller's current state.
    - **Outcome:** An agent in Lagos will never see an order from Port Harcourt, even if their distance filter is set wide.

### 2. Strict 20km Radius Lock
- **Requirement:** Limit the visibility of orders to agents within a reasonable proximity.
- **Implementation:** Added a PostGIS-powered geographic check (`ST_DWithin`) to only return orders within **20,000 meters** (20km) of the fulfiller's current location.
- **Outcome:** Fulfillers now only see nearby, profitable missions.

### 3. Background Fleet Pings (Staleness Guard)
- **The Problem:** Fulfillers who were "Online" but idle were not updating their location, leading to matching against stale data.
- **The Fix:**
    - Updated the fulfiller app (`FulfillerDashboardScreen.kt`) to send a background GPS and State ping **every 60 seconds** while marked "Online."
    - Added a **Staleness Guard** on the server: Fulfillers who haven't pinged in the last 30 minutes are automatically excluded from new order broadcasts.
    - **Outcome:** Higher dispatch precision and reduced "ghost agent" matching.

### 4. Enriched Mission Preview
- Fulfillers can now see the **exact distance (KM)** to the pickup point and the **Mission Type** (Delivery Only vs. Delivery + COD) in their list of offers before accepting.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these critical schema and dispatch logic updates to your **VPS**:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **State Isolation:** Create an order in Port Harcourt. Verify a Lagos fulfiller (after background ping) does not see it.
2. **Radius Boundary:** Move a fulfiller to 21km away from a test pickup. Verify the offer disappears from their screen.
3. **Background Ping:** Open the Fulfiller Dashboard and wait 60 seconds. Check Logcat for `[FleetPing] Background PING sent`.
