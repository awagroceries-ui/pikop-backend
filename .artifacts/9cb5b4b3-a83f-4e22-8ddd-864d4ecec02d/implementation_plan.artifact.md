# 📋 Implementation Plan: Admin Dashboard 5-Pillar Comprehensive Enhancement

Implement the 5 high-impact pillars for the Admin Dashboard and Express backend engine:
1. Real-Time Operational Command & Live Fleet Control Map (`dashboard.ejs`, `orders.ejs`, `adminController.js`).
2. Financial Intelligence, Batch Payout Approvals & Fee Simulation (`withdrawals.ejs`, `settings.ejs`, `adminController.js`).
3. Automated Fraud Detection, Shared Bank Flags & Risk Safeguards (`customers.ejs`, `emergency_dashboard.ejs`, `adminController.js`).
4. AI Support Analytics & Canned Dispute Resolutions (`support.ejs`, `dispute_resolution.ejs`, `adminController.js`).
5. Merchant Performance Heatmap & Out-of-Stock Risk Flags (`merchants.ejs`, `adminController.js`).

---

## 🔍 Detailed Component Upgrades

### Pillar 1: Real-Time Fleet Control & Manual Dispatch Override
- **Global Live Fleet Map (`dashboard.ejs`)**:
  - Integrate Leaflet map displaying live online agents (`🟢 ONLINE`), active missions (`🚗 IN TRANSIT` green route line), and unassigned searching orders (`🟡 SEARCHING` pulse).
  - Real-time Socket.IO stream (`location_updated` and `order_status_updated`).
- **Manual Dispatch Override (`orders.ejs` & `adminController.js`)**:
  - Add **"Manual Assign"** modal on `orders.ejs` allowing admins to assign any searching/stuck mission directly to an online agent.
  - Implement `POST /admin/orders/:id/assign` in `adminController.js` and `adminRoutes.js`.

### Pillar 2: Batch Payout Approvals & Fee Simulation
- **Batch Payout Approvals (`withdrawals.ejs` & `adminController.js`)**:
  - Add **"Batch Approve All Payouts"** action sending `POST /admin/withdrawals/batch-approve`.
  - Implement `batchApproveWithdrawals` in `adminController.js` executing queued Paystack transfers and audit logs.
- **Interactive Fee Simulation (`settings.ejs`)**:
  - Add interactive JS calculator on `settings.ejs` displaying live revenue impact for COD Escrow Fee, Dispatch Commission, and Merchant Commissions.

### Pillar 3: Fraud Detection & Risk Safeguards
- **Duplicate Account & Shared Bank Flags (`adminController.js`)**:
  - Automatically flag accounts sharing duplicate bank account numbers, phone numbers, or IP addresses in `getCustomers`, `getFulfillers`, and `getMerchants`.
- **Stationarity & Route Deviation Flags (`emergency_dashboard.ejs`)**:
  - Flag active orders where the assigned agent has been stationary for >20 minutes or far from the target route.

### Pillar 4: AI Support Analytics & Dispute Quick-Resolutions
- **AI Support Resolution Analytics (`support.ejs`)**:
  - Display AI Support query stats (total AI queries, resolution rate %, human escalation count) on `support.ejs`.
- **Canned Dispute Resolutions (`dispute_resolution.ejs`)**:
  - Add quick resolution templates (*"Item Damaged in Transit"*, *"Wrong Item Delivered"*, *"Receiver Unreachable"*) on `dispute_resolution.ejs`.

### Pillar 5: Merchant Performance & Store Risk Flags
- **Merchant Heatmap & Risk Flags (`merchants.ejs` & `adminController.js`)**:
  - Rank top-performing vendors and kitchens by completed volume, ratings, and return rates.
  - Flag stores set to "OPEN" that have zero active listings or high cancellation rates.

---

## 🛠️ Proposed Changes

### Component 1: Web Admin Views (`backend_v3/src/views/`)

#### [MODIFY] [dashboard.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/dashboard.ejs)
- Add interactive Leaflet Global Fleet Map with real-time Socket.IO live agent and mission markers.

#### [MODIFY] [orders.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/orders.ejs)
- Add Manual Dispatch Override modal & trigger button.

#### [MODIFY] [withdrawals.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/withdrawals.ejs)
- Add Batch Payout Approval button and modal handler.

#### [MODIFY] [settings.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/settings.ejs)
- Add interactive Fee Impact Calculator.

#### [MODIFY] [emergency_dashboard.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/emergency_dashboard.ejs)
- Add Stationarity & Route Deviation risk alert section.

#### [MODIFY] [support.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/support.ejs)
- Add AI Support Agent resolution metrics card.

#### [MODIFY] [dispute_resolution.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/dispute_resolution.ejs)
- Add Canned Quick-Resolution templates.

#### [MODIFY] [merchants.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/merchants.ejs)
- Add Merchant Performance ranking & out-of-stock risk flags.

---

### Component 2: Backend Controller & Routes (`backend_v3/src/controllers/` & `routes/`)

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement `assignOrderToAgent`, `batchApproveWithdrawals`, duplicate risk flags in user/merchant queries, and AI support metrics.

#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add `POST /admin/orders/:id/assign` and `POST /admin/withdrawals/batch-approve` routes.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Test `/admin/dashboard` -> verify real-time Leaflet fleet map loads online agents and active missions.
2. Test `/admin/orders` -> verify Manual Dispatch Override modal assigns mission to selected online agent.
3. Test `/admin/withdrawals` -> verify Batch Payout Approval executes batch Paystack transfers.
4. Test `/admin/settings` -> verify interactive fee simulator calculates revenue projections.
5. Stage, commit, and push changes to GitHub `main`.
6. Deploy to production VPS server (`api.pikop.com.ng`) and restart PM2.
