# 🚀 Walkthrough: Merchant Verification Approval Fix (`approved_at`)

Resolved the Admin Dashboard error `column "approved_at" does not exist` when approving Merchants (Vendors & Kitchens) in the Verification Queue.

---

## 🛠️ Summary of Changes

### 1. Database Migration
- Created [1726920000000_add_approved_at_to_vendors_and_kitchens.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726920000000_add_approved_at_to_vendors_and_kitchens.js):
  - Adds `approved_at` (`timestamp`) column to both `vendors` and `kitchens` tables.

### 2. Admin Controller
- Updated [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js):
  - Made `updateMerchantKYCStatus` parameter parsing resilient to both `req.params` and `req.body`.
  - Ensures status updates (`active` / `suspended`), `approved_at` timestamp setting, user role upgrades (`MERCHANT`), and welcome emails run smoothly without SQL errors.

---

## 🧪 Git Automation & Deployment

- Changes committed (`57b8479c`) and pushed to GitHub `origin/main`.
- Deploy to VPS server using commands below.
