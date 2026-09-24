# 📌 Task Checklist: Fleet Deletion, Online Indicators & Merchant Management

- `[/]` Task 1: Fix User Account Force Delete (`forceDeleteUser`)
  - `[ ]` Implement `SAVEPOINT` wrapper helper (`safeExec`) in `adminController.js`
  - `[ ]` Safely clean up FK dependencies (`orders`, `wallets`, `withdrawals`, `emergency_alerts`, `kyc_documents`, `corporate_sub_accounts`, `fleet_partner_invites`)
  - `[ ]` Ensure `DELETE FROM users WHERE id = $1` executes cleanly and commits

- `[ ]` Task 2: Add Fleet Management Online/Offline Indicator
  - `[ ]` Update `fulfillers.ejs` to include `🟢 ONLINE` / `⚪ OFFLINE` indicator column
  - `[ ]` Update `fulfiller_detail.ejs` to display real-time `online_status` badge in profile card

- `[ ]` Task 3: Admin Merchant Management (Suspend, Ban, Delete)
  - `[ ]` Implement `deleteMerchant` in `adminController.js`
  - `[ ]` Add route `POST /admin/merchants/:type/:id/delete` in `adminRoutes.js`
  - `[ ]` Update `vendors.ejs`, `kitchens.ejs`, and `merchants.ejs` with action buttons for **Suspend**, **Ban**, **Reactivate**, and **Delete**

- `[ ]` Task 4: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
