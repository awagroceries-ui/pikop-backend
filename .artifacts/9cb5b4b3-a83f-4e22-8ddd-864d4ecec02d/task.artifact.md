# 📌 Task Checklist: Real-Road Live Tracking Map Route, Solid Green Lines & Marker Animation

- `[/]` Task 1: Create `RoadRouteService.kt`
  - `[ ]` Implement `fetchRoadRoute(start, end, apiKey)` with OSRM & Google Directions fallback
  - `[ ]` Implement `decodePolyline(encoded)` decoder for real street coordinates
  - `[ ]` Implement `interpolatePointAlongPolyline(points, fraction)` for smooth along-the-route movement

- `[ ]` Task 2: Update Customer Live Order Tracking (`TrackOrderScreen.kt`)
  - `[ ]` Asynchronously fetch real-road street route points
  - `[ ]` Render solid brand green polyline (`Color(0xFF00E676)`, `width = 14f`, `JointType.ROUND`, `RoundCap()`)
  - `[ ]` Animate agent marker traveling along the real-road polyline path

- `[ ]` Task 3: Update Fulfiller Active Mission Screen (`ActiveOrderScreen.kt`)
  - `[ ]` Asynchronously fetch real-road street route points to pickup and dropoff destinations
  - `[ ]` Render solid brand green polyline (`Color(0xFF00E676)`, `width = 14f`, `JointType.ROUND`, `RoundCap()`)

- `[ ]` Task 4: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device

- `[ ]` Task 5: Git Automation
  - `[ ]` Stage, commit, and push changes to GitHub `main`
