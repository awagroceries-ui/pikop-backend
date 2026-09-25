# 📌 Task Checklist: Instant Dispatch, Agent Live GPS Map & Mission Records Real-Time Sync

- `[/]` Task 1: Backend Dispatch Engine & Socket Emission
  - `[ ]` Update `findNearbyFulfillers` in `dispatchService.js` to handle null state/location and include all online verified agents
  - `[ ]` Broadcast `new_mission_offer` to target users and `online_fulfillers` socket room in `dispatchService.js`
  - `[ ]` Emit `order_status_updated` to `user_${userId}` in `acceptOrder` and `promoteQueuedMission` in `orderController.js`
  - `[ ]` Ensure online fulfillers join `online_fulfillers` room in `socketService.js`

- `[ ]` Task 2: Fulfiller Dashboard Live GPS Map & Real-Time Sync (`FulfillerDashboardScreen.kt`)
  - `[ ]` Implement robust location resolution (`lastLocation` -> `getCurrentLocation(HIGH_ACCURACY)`)
  - `[ ]` Center map camera on agent's coordinates (`zoom 14f`), draw "Your Location" blue marker, and send status location ping
  - `[ ]` Keep camera centered on agent's city when hotspots are toggled
  - `[ ]` Add 5-second polling loop for `getFulfillerOrders()` and `getOffers()`
  - `[ ]` Connect Socket.IO listeners (`new_mission_offer`, `order_status_updated`) to refresh dashboard data instantly on socket events

- `[ ]` Task 3: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 4: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
