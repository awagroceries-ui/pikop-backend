# 📌 Task Checklist: Admin Dashboard 5-Pillar Comprehensive Enhancement

- `[/]` Task 1: Pillar 1 - Real-Time Fleet Control & Manual Dispatch Override
  - `[ ]` Add real-time Leaflet Global Fleet Map to `dashboard.ejs`
  - `[ ]` Add Manual Dispatch Override modal to `orders.ejs`
  - `[ ]` Add `assignOrderToAgent` controller function in `adminController.js` & route `POST /admin/orders/:id/assign` in `adminRoutes.js`

- `[ ]` Task 2: Pillar 2 - Batch Payout Approvals & Fee Simulation
  - `[ ]` Add Batch Payout Approval button to `withdrawals.ejs`
  - `[ ]` Add `batchApproveWithdrawals` controller function in `adminController.js` & route `POST /admin/withdrawals/batch-approve` in `adminRoutes.js`
  - `[ ]` Add interactive Fee Impact Simulator to `settings.ejs`

- `[ ]` Task 3: Pillar 3 - Fraud Detection & Risk Safeguards
  - `[ ]` Add shared bank account / duplicate phone risk flags to `adminController.js`
  - `[ ]` Add stationarity & route deviation risk section to `emergency_dashboard.ejs`

- `[ ]` Task 4: Pillar 4 & 5 - AI Support Analytics, Canned Dispute Templates & Merchant Heatmap
  - `[ ]` Add AI Support Agent resolution metrics to `support.ejs`
  - `[ ]` Add canned quick-resolution templates to `dispute_resolution.ejs`
  - `[ ]` Add merchant performance ranking & out-of-stock risk flags to `merchants.ejs`

- `[ ]` Task 5: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
