# 🚀 Walkthrough: Admin Dashboard & Guest Real-Road Live Tracking Map Upgrade

Upgraded the Admin Dashboard Live Mission Tracking view (`admin_track.ejs`) and Guest Live Tracking view (`guest_tracking.ejs`) with real-road OSRM street network routes (following actual streets and turns), solid brand green route polylines (`#00E676`), and smooth live agent marker animation.

---

## 🛠️ Summary of Implementation

### 1. Web Admin Live Tracking View (`admin_track.ejs`)
- Updated [admin_track.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/admin_track.ejs):
  - **OSRM Real-Road GeoJSON Engine**: Integrated `renderRealRoadRoute(start, end)` querying the OSRM street network routing API (`https://router.project-osrm.org/route/v1/driving/`).
  - **Solid Brand Green Route**: Renders primary mission routes in **solid brand green** (`color: '#00E676'`, `weight: 5`, `opacity: 0.95`, `lineCap: 'round'`, `lineJoin: 'round'`).
  - **Live Agent Route Segment**: Dynamically fetches and updates the real-road route segment from the agent's live position to the target destination (pickup or dropoff) in solid brand green.
  - **Marker Gliding**: Smoothly glides `agentMarker` along the real-road polyline on Socket.IO `location_updated` events.

### 2. Guest Public Live Tracking View (`guest_tracking.ejs`)
- Updated [guest_tracking.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_tracking.ejs):
  - Integrated OSRM real-road GeoJSON routing to display public tracking routes in **solid brand green** (`#00E676`).
  - Smoothly glides live agent marker along actual street turns on socket `location_updated` events.

---

## 🧪 VPS Deployment Instructions

Run the command below on your VPS terminal (`root@srv1932412`) to pull the update and restart PM2:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
pm2 logs pikop-v3 --lines 30
```
