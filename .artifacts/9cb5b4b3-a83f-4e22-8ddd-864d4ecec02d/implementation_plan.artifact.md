# 📋 Implementation Plan: Real-Road Live Tracking Map Route, Solid Green Lines & Marker Animation

Upgrade live tracking and mission navigation maps in `TrackOrderScreen.kt` and `ActiveOrderScreen.kt` to display accurate real-road street routes (following actual streets and turns) with solid brand green lines (`Color(0xFF00E676)`) and smooth marker animation along the polyline path.

---

## 🔍 Requirements & Technical Solution

1. **Accurate Real-Road Route Engine**:
   - Replace fake L-shaped curve generator (`createRoadPolyline`) with real-road network routing using Google Maps Directions API / OSRM routing engine.
   - Fetch real street polylines using `origin` and `destination` GPS coordinates.
   - Decode Google encoded `overview_polyline` strings into precise `List<LatLng>` coordinates following actual streets, turns, intersections, and roundabouts.

2. **Solid Brand Green Route Line**:
   - Render routes as solid, vibrant brand green lines (`Color(0xFF00E676)`, `width = 14f`).
   - Use smooth rounded caps and joints (`JointType.ROUND`, `RoundCap()`).
   - Remove all dotted/dashed gray line patterns (`Dash`, `Gap`).

3. **Smooth Marker Animation Along Polyline**:
   - Implement `interpolatePointAlongPolyline(points: List<LatLng>, fraction: Float): LatLng` to calculate exact position along the multi-point street path.
   - Animate the agent vehicle marker smoothly along the green polyline path as location updates arrive or progress advances.

---

## 🛠️ Proposed Changes

### Component 1: Real-Road Route Helper & Polyline Decoder

#### [NEW] [RoadRouteService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/RoadRouteService.kt)
- `fetchRoadRoute(start: LatLng, end: LatLng, apiKey: String): List<LatLng>`: Fetches real street route polylines via Google Maps Directions API / OSRM and decodes `overview_polyline`.
- `decodePolyline(encoded: String): List<LatLng>`: Lightweight, fast Google Polyline algorithm decoder.
- `interpolatePointAlongPolyline(points: List<LatLng>, fraction: Float): LatLng`: Calculates exact intermediate coordinates at any progress fraction along the polyline path.

---

### Component 2: Customer Order Tracking (`TrackOrderScreen.kt`)

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Asynchronously fetch real-road street route points (`RoadRouteService.fetchRoadRoute`).
- Render solid brand green polyline (`Color(0xFF00E676)`, `width = 14f`, `JointType.ROUND`).
- Animate agent marker traveling along the street polyline turns.

---

### Component 3: Fulfiller Mission Navigation (`ActiveOrderScreen.kt`)

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Fetch real-road street route points from agent's live GPS position to pickup/dropoff target.
- Render solid brand green polyline (`Color(0xFF00E676)`, `width = 14f`, `JointType.ROUND`).

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Open Customer Live Order Tracking (`TrackOrderScreen.kt`) on connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
2. Verify route line follows actual streets and turns (not fake L-shaped lines).
3. Verify route is a solid, vibrant brand green line (`Color(0xFF00E676)`).
4. Verify agent marker animates smoothly along the green line.
5. Open Agent Active Mission (`ActiveOrderScreen.kt`) -> verify navigation polyline follows real streets in solid green.
6. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to device.
7. Stage, commit, and push changes to GitHub `main`.
