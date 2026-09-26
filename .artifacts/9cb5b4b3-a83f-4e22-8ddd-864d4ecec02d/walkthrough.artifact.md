# 🚀 Walkthrough: Restore All Active & Queued Missions for Agent Fulfillment

Created the `restore_missions.js` utility script and `/admin/orders/restore-all` admin endpoint to audit, normalize, and restore all active and queued delivery missions in PostgreSQL to their designated agents.

---

## 🛠️ Summary of Implementation

### 1. Database Mission Restoration Script (`restore_missions.js`)
- Created [restore_missions.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/restore_missions.js):
  - Audits all non-completed orders in PostgreSQL (`status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED')`).
  - Restores status to **`MATCHED`** for active orders assigned to a `fulfiller_id`.
  - Promotes queued missions (`queued_for_fulfiller_id`) to **`MATCHED`** if the agent is currently free, or normalizes status to **`QUEUED`** if the agent is busy on an active mission.
  - Emits real-time Socket.IO events (`status_updated`, `order_status_updated`, `new_mission_offer`) so connected agent dashboards update immediately.

### 2. Admin Controller & Route (`adminController.js` & `adminRoutes.js`)
- Updated [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js) and [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js):
  - Added `POST /admin/orders/restore-all` endpoint for manual restoration triggers from the Admin Dashboard.

---

## 🧪 VPS Deployment & Execution Instructions

Run the command below on your VPS terminal (`root@srv1932412`) to pull the update, execute the restoration script, and restart PM2:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
node restore_missions.js
pm2 restart pikop-v3
pm2 logs pikop-v3 --lines 30
```
