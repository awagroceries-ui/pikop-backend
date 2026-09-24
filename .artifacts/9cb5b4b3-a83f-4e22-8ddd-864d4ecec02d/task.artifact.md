# 📌 Task Checklist: Fleet Deletion, Online Indicators & Merchant Management

- `[x]` Task 1: Fix User Account Force Delete (`forceDeleteUser`)
  - `[x]` Implement `SAVEPOINT` wrapper helper (`safeExec`) in `adminController.js`
  - `[x]` Safely clean up FK dependencies (`orders`, `wallets`, `withdrawals`, `emergency_alerts`, `kyc_documents`, `corporate_sub_accounts`, `fleet_partner_invites`)
  - `[x]` Ensure `DELETE FROM users WHERE id = $1` executes cleanly and commits

- `[x]` Task 2: Add Fleet Management Online/Offline Indicator
  - `[x]` Update `fulfillers.ejs` to include `🟢 ONLINE` / `⚪ OFFLINE` indicator column
  - `[x]` Update `fulfiller_detail.ejs` to display real-time `online_status` badge in profile card

- `[x]` Task 3: Admin Merchant Management (Suspend, Ban, Delete)
  - `[x]` Implement `deleteMerchant` in `adminController.js`
  - `[x]` Add route `POST /admin/merchants/:type/:id/delete` in `adminRoutes.js`
  - `[x]` Update `vendors.ejs`, `kitchens.ejs`, and `merchants.ejs` with action buttons for **Suspend**, **Ban**, **Reactivate**, and **Delete**

- `[x]` Task 4: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
