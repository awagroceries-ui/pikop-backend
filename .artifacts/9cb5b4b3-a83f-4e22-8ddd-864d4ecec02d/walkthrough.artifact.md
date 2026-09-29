# 🚀 Walkthrough: Resolved Map Tile 403 Access Blocked Error

Replaced OpenStreetMap's volunteer tile server URL in `dashboard.ejs` and `guest_tracking.ejs` with Esri World Street Map production tiles (`https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}`), eliminating OpenStreetMap's `403 Access Blocked` usage policy block.

---

## 🛠️ Summary of Implementation

### 1. Global Fleet Map Tile Provider Update (`dashboard.ejs`)
- Updated [dashboard.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/dashboard.ejs):
  - Swapped `tile.openstreetmap.org` tile layer for Esri World Street Map tiles (`https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}`).
  - Resolves OpenStreetMap's `403 Access Blocked` tile server policy error and renders high-definition, unblocked street map tiles on the Admin Dashboard.

### 2. Guest Live Tracking Map Tile Provider Update (`guest_tracking.ejs`)
- Updated [guest_tracking.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_tracking.ejs):
  - Swapped `tile.openstreetmap.org` tile layer for Esri World Street Map tiles.

---

## 🧪 VPS Deployment Instructions

Run the command below on your VPS terminal (`root@srv1932412`) to pull the tile fix and restart PM2:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
pm2 logs pikop-v3 --lines 30
```
