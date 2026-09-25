# 📌 Task Checklist: Restore All Active & Queued Missions for Agent Fulfillment

- `[/]` Task 1: Create Database Restoration Script (`restore_missions.js`)
  - `[ ]` Write `restore_missions.js` to audit non-completed orders in PostgreSQL
  - `[ ]` Restore assigned active missions to `MATCHED` or current in-progress state
  - `[ ]` Promote or queue queued missions (`QUEUED` / `MATCHED`)
  - `[ ]` Broadcast real-time Socket.IO events (`status_updated`, `order_status_updated`, `new_mission_offer`)
  - `[ ]` Execute `node restore_missions.js` on local environment

- `[ ]` Task 2: Add Admin Endpoint (`adminController.js` & `adminRoutes.js`)
  - `[ ]` Add `restoreAllMissions` function to `adminController.js`
  - `[ ]` Add `POST /admin/orders/restore-all` route to `adminRoutes.js`

- `[ ]` Task 3: Git Automation & VPS Execution
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
