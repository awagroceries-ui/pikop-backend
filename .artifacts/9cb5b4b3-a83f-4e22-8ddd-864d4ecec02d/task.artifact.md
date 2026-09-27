# 📌 Task Checklist: Admin Dashboard 5-Pillar Comprehensive Enhancement

- `[x]` Task 1: Pillar 1 - Real-Time Fleet Control & Manual Dispatch Override
  - `[x]` Add real-time Leaflet Global Fleet Map to `dashboard.ejs`
  - `[x]` Add Manual Dispatch Override modal to `orders.ejs`
  - `[x]` Add `assignOrderToAgent` controller function in `adminController.js` & route `POST /admin/orders/:id/assign` in `adminRoutes.js`

- `[x]` Task 2: Pillar 2 - Batch Payout Approvals & Fee Simulation
  - `[x]` Add Batch Payout Approval button to `withdrawals.ejs`
  - `[x]` Add `batchApproveWithdrawals` controller function in `adminController.js` & route `POST /admin/withdrawals/batch-approve` in `adminRoutes.js`
  - `[x]` Add interactive Fee Impact Simulator to `settings.ejs`

- `[x]` Task 3: Pillar 3 - Fraud Detection & Risk Safeguards
  - `[x]` Add shared bank account / duplicate phone risk flags to `adminController.js`
  - `[x]` Add stationarity & route deviation risk section to `emergency_dashboard.ejs`

- `[x]` Task 4: Pillar 4 & 5 - AI Support Analytics, Canned Dispute Templates & Merchant Heatmap
  - `[x]` Add AI Support Agent resolution metrics to `support.ejs`
  - `[x]` Add canned quick-resolution templates to `dispute_resolution.ejs`
  - `[x]` Add merchant performance ranking & out-of-stock risk flags to `merchants.ejs`

- `[x]` Task 5: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
