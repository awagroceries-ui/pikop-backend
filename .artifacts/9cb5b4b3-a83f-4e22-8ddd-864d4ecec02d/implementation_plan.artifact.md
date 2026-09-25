# 📋 Implementation Plan: Instant Dispatch, Agent Live GPS Map & Mission Records Real-Time Sync

Resolve real-time mission dispatch latency, fix the Hotspot Map locking to Lagos, and ensure accepted/queued missions appear and update instantly for agents.

---

## 🔍 Root Cause Analysis & Plan Overview

### 1. Instant Mission Dispatch (`dispatchService.js` & `socketService.js`)
- **Problem**: `findNearbyFulfillers` query strictly required `f.current_state ILIKE $3` and `f.last_ping_at > NOW() - interval '30 minutes'`. When `current_state` or `current_location` was null or state strings mismatched (`Port Harcourt` vs `Rivers`), 0 fulfillers were returned and `broadcastOffer` exited without sending offers.
- **Fix**:
  - Relax `findNearbyFulfillers` WHERE clause to match `(f.current_state IS NULL OR f.current_state ILIKE $3 OR ST_DWithin(...))` for all verified online agents.
  - Broadcast `new_mission_offer` to `user_${f.user_id}` and `online_fulfillers` socket room so every online agent receives real-time UI popups instantly.

### 2. Hotspot Map & Live GPS Resolution (`FulfillerDashboardScreen.kt`)
- **Problem**: Camera initialized to hardcoded Lagos (`6.5244, 3.3792`). Location fetch used `PRIORITY_BALANCED_POWER_ACCURACY` without `lastLocation` fallback, failing silently. Loading hotspots overrode the camera position to `parsedHotspots.first()`. Agent marker was missing.
- **Fix**:
  - Fetch agent location with fallback chain: `lastLocation.await()` -> `getCurrentLocation(PRIORITY_HIGH_ACCURACY)`.
  - Center camera on agent's actual location (`zoom 14f`), draw **"Your Location"** marker, and send background location ping (`updateStatus`) to update server `current_location` and `current_state`.
  - Draw hotspot markers on map without forcibly panning camera away from the agent's city.

### 3. Mission Records Real-Time Sync (`FulfillerDashboardScreen.kt` & `orderController.js`)
- **Problem**: Polling loop in `FulfillerDashboardScreen.kt` stalled if GPS returned null. Socket listeners for `new_mission_offer` and `order_status_updated` were not attached to the Dashboard screen. `acceptOrder` did not emit socket updates to `user_${userId}`.
- **Fix**:
  - Add robust `delay(5000)` polling loop in `FulfillerDashboardScreen.kt` that refreshes `history` and `offers` continuously.
  - Attach Socket.IO listeners on `FulfillerDashboardScreen` for `new_mission_offer`, `order_status_updated`, and `mission_promoted` to trigger `fetchDashboardData()` instantly.
  - In `orderController.js` (`acceptOrder` & `promoteQueuedMission`), emit `order_status_updated` to `user_${userId}` and `order_${orderId}`.

---

## 🛠️ Proposed Changes

### Component 1: Backend API & Dispatch Engine (`backend_v3`)

#### [MODIFY] [dispatchService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/dispatchService.js)
- Update `findNearbyFulfillers` SQL query to handle null `current_state` / `current_location` and include all active online verified fulfillers.
- In `broadcastOffer`, emit `new_mission_offer` to target users and broadcast room.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- In `acceptOrder` and `promoteQueuedMission`, emit `order_status_updated` to `user_${userId}` and `order_${orderId}`.

#### [MODIFY] [socketService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/socketService.js)
- Ensure Fulfillers automatically join `online_fulfillers` room when toggled online.

---

### Component 2: Android Mobile App (`:app`)

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Implement location resolution fallback (`lastLocation` -> `getCurrentLocation(HIGH_ACCURACY)`).
- Center map on agent's live coordinates, draw blue **"Your Location"** marker, and send status location ping.
- Keep camera centered on agent's city when hotspots are toggled.
- Fix polling loop with 5-second interval for `getFulfillerOrders()` and `getOffers()`.
- Attach Socket.IO listeners (`new_mission_offer`, `order_status_updated`) to refresh dashboard data instantly on socket events.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Test location resolution on connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
2. Verify Dashboard map centers on agent's live location with blue marker on entry (no Lagos lock).
3. Test placing a new order on customer app -> verify instant `new_mission_offer` popup and sound alert on agent dashboard.
4. Accept mission -> verify active mission card ("RESUME") and queued mission card ("START") appear immediately on agent dashboard and Mission Records screen.
5. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to device.
6. Stage, commit, and push changes to GitHub `main`.
7. Deploy to production VPS server (`api.pikop.com.ng`) and restart PM2.
