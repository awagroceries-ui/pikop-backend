# 📋 Implementation Plan: Admin Dashboard Real-Road Live Mission Tracking Upgrade

Upgrade the Admin Dashboard Live Mission Tracking view (`admin_track.ejs`) and Guest Tracking view (`guest_tracking.ejs`) with real-road OSRM street network routes (following actual streets and turns), solid brand green route polylines (`#00E676`), and smooth live agent marker animation.

---

## 🔍 Requirements & Technical Solution

1. **Real-Road OSRM Street Network Routing**:
   - Query OSRM real-road driving routing engine (`https://router.project-osrm.org/route/v1/driving/`) using `pickup` and `delivery` coordinates.
   - Parse GeoJSON geometry following actual streets, turns, intersections, and roundabouts.

2. **Solid Brand Green Route Lines**:
   - Render the route with `L.geoJSON` as a solid, vibrant brand green line (`color: '#00E676'`, `weight: 5`, `opacity: 0.95`, `lineCap: 'round'`, `lineJoin: 'round'`).
   - Remove any plain straight lines or dotted/dashed patterns.

3. **Live Agent Active Route & Smooth Marker Gliding**:
   - When active agent coordinates (`agentPos`) are present or updated via Socket.IO (`location_updated`), fetch active road segment from agent position to target destination (pickup or delivery) in solid brand green.
   - Smoothly glide the agent marker along the real-road street polyline.

---

## 🛠️ Proposed Changes

### Component 1: Web Admin & Tracking Views (`backend_v3`)

#### [MODIFY] [admin_track.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/admin_track.ejs)
- Integrate OSRM real-road GeoJSON route fetcher for pickup and delivery locations.
- Render solid brand green route polyline (`color: '#00E676'`, `weight: 5`).
- Fetch and render agent active route segment to target destination in solid brand green.
- Smoothly glide agent marker along the real-road polyline on socket `location_updated` events.

#### [MODIFY] [guest_tracking.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_tracking.ejs)
- Integrate OSRM real-road GeoJSON route fetcher for delivery tracking.
- Render solid brand green route polyline (`color: '#00E676'`, `weight: 5`).
- Smoothly glide agent marker on socket `location_updated` events.

---

## 🧪 Verification Plan

### Execution & Verification Steps
1. Test `/admin/orders/:id/track` on local or production server.
2. Verify route line follows actual streets and turns in solid brand green (`#00E676`).
3. Verify agent marker glides smoothly along the green route line on live socket location updates.
4. Stage, commit, and push changes to GitHub `main`.
5. Deploy to production VPS server (`api.pikop.com.ng`) and restart PM2.
