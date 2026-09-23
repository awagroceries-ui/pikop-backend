# 📋 Implementation Plan: Merchant Hub Verification Queue "approved_at" Column Fix

Fix the Admin Dashboard Merchant Hub Verification Queue approval crash (`column "approved_at" does not exist`).

---

## 🔍 Root Cause Analysis

In `adminController.js` (`updateMerchantKYCStatus`), when an Admin approves or rejects a Merchant (Vendor or Kitchen), the controller executes:
```sql
UPDATE ${table} SET status = $1, approved_at = CASE WHEN $2 = 'VERIFIED' THEN CURRENT_TIMESTAMP ELSE approved_at END WHERE id = $3
```
where `${table}` is either `vendors` or `kitchens`.

Unlike `fulfillers`, neither the `vendors` table nor the `kitchens` table has an `approved_at` column in the database schema. PostgreSQL rejects the query with:
`error: column "approved_at" of relation "vendors" (or "kitchens") does not exist`
resulting in an HTTP 500 error on the Admin Dashboard.

---

## 🛠️ Proposed Changes

### Database Migration

#### [NEW] [1726920000000_add_approved_at_to_vendors_and_kitchens.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726920000000_add_approved_at_to_vendors_and_kitchens.js)
- Add `approved_at` (`timestamp`) column to both `vendors` and `kitchens` tables if not already present.

---

### Backend Admin Controller

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `updateMerchantKYCStatus` to ensure `approved_at` is set cleanly upon approval and error handling is robust.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Run database migration (`npm run migrate:up`).
2. Verify `vendors` and `kitchens` tables include `approved_at` column.
3. Test approving a merchant in the Admin Verification Queue (`/admin/merchant-kyc` or `/admin/vendors`).
4. Confirm status changes to `active`, `approved_at` is set, user role is upgraded to `MERCHANT`, and welcome email is sent without 500 errors.
5. Commit, push to GitHub `main`, and execute VPS deployment.
