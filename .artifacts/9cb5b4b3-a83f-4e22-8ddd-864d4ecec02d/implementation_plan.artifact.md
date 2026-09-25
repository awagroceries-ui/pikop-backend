# 📋 Implementation Plan: Restore All Active & Queued Missions for Agent Fulfillment

Audit and restore all active, assigned, and queued delivery missions in PostgreSQL to their designated agents, ensuring statuses are normalized (`MATCHED`, `QUEUED`, `PICKED_UP`, `IN_TRANSIT`), and broadcast real-time socket events so agents can immediately execute or resume them.

---

## 🔍 Research & Problem Analysis

1. **Mission Assignment State**:
   - Missions in PostgreSQL `orders` table have two key fulfillment columns:
     - `fulfiller_id`: Primary agent assigned to execute the mission.
     - `queued_for_fulfiller_id`: Agent queued to execute the mission next once their active mission completes.
   - If an order's status was left in `SEARCHING`, `PENDING_ACKNOWLEDGMENT`, or `PENDING` despite having `fulfiller_id` or `queued_for_fulfiller_id` set, the agent's dashboard status filters omitted it.

2. **Fulfillment Restoration Logic**:
   - **Assigned Active Missions**: If an order has `fulfiller_id` set and status is `SEARCHING`, `PENDING`, or `PENDING_ACKNOWLEDGMENT`, restore status to `MATCHED` so it appears as an **ACTIVE MISSION IN PROGRESS** on the agent's dashboard and mission records screen.
   - **Queued Missions**: If an order has `queued_for_fulfiller_id` set and `fulfiller_id` is null, check if the agent currently has an active mission:
     - If the agent has no active mission: Promote order to `fulfiller_id = queued_for_fulfiller_id` and set `status = 'MATCHED'`.
     - If the agent is currently busy on an active mission: Set `status = 'QUEUED'`.
   - **Unassigned Searching Missions**: Broadcast real-time `new_mission_offer` socket events to all online verified agents (`online_fulfillers` and `user_${user_id}`).

---

## 🛠️ Proposed Changes

### Component 1: Backend Restoration Script & Admin Control (`backend_v3`)

#### [NEW] [restore_missions.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/restore_missions.js)
- Standalone Node.js database restoration script that:
  - Connects to PostgreSQL `pikop` database.
  - Queries all non-completed/non-cancelled orders (`status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED')`).
  - Restores status and assignment mappings for active and queued fulfillers.
  - Emits real-time Socket.IO events (`status_updated`, `order_status_updated`, `new_mission_offer`) to online agent sockets.
  - Prints a detailed summary report of restored missions.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js) & [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add `/admin/orders/restore-all` POST endpoint to allow manual trigger from Admin Dashboard.

---

## 🧪 Verification Plan

### Execution & Verification Steps
1. Execute `node restore_missions.js` on local project directory.
2. Stage, commit, and push `restore_missions.js` and controller updates to GitHub `main`.
3. Deploy to production VPS server (`root@srv1932412`).
4. Execute `node restore_missions.js` on VPS production environment.
5. Verify on connected device (**Samsung Galaxy S23 Ultra**):
   - Agent dashboard displays active mission ("RESUME") and queued mission ("START") cards cleanly.
   - Mission Records screen displays restored active/queued items.
