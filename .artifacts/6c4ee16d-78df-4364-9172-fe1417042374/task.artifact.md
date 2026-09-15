# Task Checklist: System Audit Fixes & Enhancements

- `[x]` **1. Legal Module Stability**
  - `[x]` Fix `ReferenceError` in `legalController.js` by passing nulls for admin context.
  - `[x]` Enhance markdown conversion logic (standardized newlines).
- `[x]` **2. Admin Visibility & Merchant Approval**
  - `[x]` Update `adminController.js` `getVendors` and `getKitchens` queries.
  - `[x]` Implement `getMerchants` unified view.
  - `[x]` Update `vendors.ejs` and `kitchens.ejs` with new columns and Approve button.
  - `[x]` Link `/admin/merchants` in `adminRoutes.js`.
- `[x]` **3. Financial Configurability**
  - `[x]` Create migration to seed commission rates into `settings` table.
  - `[x]` Update `platform.js` to prioritize configurable rates.
  - `[x]` Update `adminController.js` and `settings.ejs` to support commission editing.
  - `[x]` Update `commerceController.js` to fetch rates from DB at runtime.
- `[x]` **4. Android Robustness**
  - `[x]` Update `ApiService.kt` DTOs with default values.
- `[ ]` **5. Verification & Git**
  - `[ ]` Build and test.
  - `[ ]` Git commit and push.
