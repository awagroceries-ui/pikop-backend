# 📋 Implementation Plan: Fleet Account Deletion, Online Indicators & Merchant Management

Address three operational issues across the Pikop Admin Dashboard:
1. Fix user/agent force deletion transaction abort error when deleting user accounts.
2. Add online/offline status indicators (`ONLINE` / `OFFLINE`) for agents in Fleet Management views.
3. Add full Merchant Management capabilities (Suspend, Ban, Reactivate, Delete) across Vendors, Kitchens, and Merchant Hub.

---

## 🔍 Root Cause Analysis & Plan

### Issue 1: Fleet Account Deletion Error (`dr-keller@hotmail.com`)
- **Root Cause**: `forceDeleteUser` in `adminController.js` wraps cascading sub-deletion queries in a single PostgreSQL `client.query('BEGIN')` transaction. If any sub-query fails (e.g. missing optional table, foreign key constraint on `orders`, `wallets`, `withdrawals`, `emergency_alerts`, `corporate_sub_accounts`), PostgreSQL marks the entire transaction block as **ABORTED**. Subsequent SQL statements fail with `"current transaction is aborted, commands ignored until end of transaction block"`.
- **Fix**:
  - Wrap each sub-operation inside `SAVEPOINT` / `RELEASE SAVEPOINT` / `ROLLBACK TO SAVEPOINT` blocks so sub-query failures don't abort the outer transaction.
  - Safely clean up all foreign key dependencies for `users` and `fulfillers` (e.g. unlink or delete associated `orders`, `wallets`, `withdrawals`, `emergency_alerts`, `kyc_documents`, `corporate_sub_accounts`, `fleet_partner_invites`).

### Issue 2: Fleet Management Online/Offline Indicator
- **Root Cause**: `fulfillers.ejs` and `fulfiller_detail.ejs` display Fulfiller name, role, KYC status, and account status, but do not show their real-time `online_status` (`ONLINE` vs `OFFLINE`).
- **Fix**:
  - Add an **Online Status** indicator badge in `fulfillers.ejs` and `fulfiller_detail.ejs`:
    - `🟢 ONLINE` (Green badge) when `online_status === 'ONLINE'`.
    - `⚪ OFFLINE` (Gray badge) when `online_status !== 'ONLINE'`.

### Issue 3: Admin Merchant Management (Suspend, Ban, Delete)
- **Root Cause**: Admin Dashboard views (`vendors.ejs`, `kitchens.ejs`, `merchants.ejs`) only include an "APPROVE" button for non-active merchants. Admins currently have no UI options to **Suspend**, **Ban**, or **Permanently Delete** a merchant (Vendor or Kitchen).
- **Fix**:
  - Add `deleteMerchant` controller method in `adminController.js`:
    - Clean up products / menu items, coupons, and returns associated with the target vendor or kitchen.
    - Delete the `vendors` or `kitchens` record safely and write audit log.
  - Add route `POST /admin/merchants/:type/:id/delete` in `adminRoutes.js`.
  - Update `vendors.ejs`, `kitchens.ejs`, and `merchants.ejs` to include status action buttons (**Suspend**, **Ban**, **Reactivate**) and a **Delete** action with confirmation prompt.

---

## 🛠️ Proposed Changes

### Backend Controllers & Routes

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `forceDeleteUser`:
  - Implement `SAVEPOINT` wrapper helper (`safeExec`) for cascading sub-deletions.
  - Clean up all FK relationships (`orders`, `wallets`, `withdrawals`, `emergency_alerts`, `kyc_documents`, `corporate_sub_accounts`, `fleet_partner_invites`, `ratings_reviews`).
  - Delete user cleanly and commit.
- Add `deleteMerchant`:
  - Handle deletion of `vendors` or `kitchens` and associated items (`products` / `menu_items`, `merchant_coupons`, `marketplace_returns`).

#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add route `router.post('/merchants/:type/:id/delete', adminController.deleteMerchant);`.

---

### Admin Dashboard Views

#### [MODIFY] [fulfillers.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fulfillers.ejs)
- Add **Online Status** column & badge (`🟢 ONLINE` / `⚪ OFFLINE`).

#### [MODIFY] [fulfiller_detail.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fulfiller_detail.ejs)
- Add **Online Status** badge header in profile card.

#### [MODIFY] [vendors.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/vendors.ejs)
- Add action buttons for **Suspend**, **Ban**, **Reactivate**, and **Delete Vendor**.

#### [MODIFY] [kitchens.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kitchens.ejs)
- Add action buttons for **Suspend**, **Ban**, **Reactivate**, and **Delete Kitchen**.

#### [MODIFY] [merchants.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/merchants.ejs)
- Add action buttons for **Suspend**, **Ban**, **Reactivate**, and **Delete Merchant**.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. **User Force Delete Test**:
   - Force delete user `dr-keller@hotmail.com` via Admin Dashboard.
   - Confirm transaction completes without transaction abort error and user/fulfiller records are purged.
2. **Fleet Online Status Test**:
   - Open `/admin/fulfillers` and `/admin/fulfillers/:id`.
   - Verify `🟢 ONLINE` / `⚪ OFFLINE` status badges render correctly based on `online_status`.
3. **Merchant Management Test**:
   - Test Suspend, Ban, Reactivate, and Delete on `/admin/vendors`, `/admin/kitchens`, and `/admin/merchants`.
   - Confirm status updates and deletion operate cleanly.
4. **Git & Deployment**:
   - Stage, commit, and push to GitHub `main`.
   - Restart PM2 on VPS and test.
