# Implementation Plan - System Audit Fixes & Enhancements

This plan addresses the bugs and inconsistencies identified during the system audit, focusing on legal page stability, admin visibility, and financial configurability.

## User Review Required

> [!CAUTION]
> **Admin Dashboard Overhaul**
> I will be making significant updates to the Admin views for Merchants and Kitchens to support the new onboarding fields. I will also consolidate the `/admin/merchants` route to act as a proper entry point.

## Proposed Changes

### 1. Legal Module Stability (Bug Fix)
#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- Explicitly pass `adminUsername: null` and `role: null` to `res.render` to prevent `ReferenceError` if the layout override fails or leaks.
- Enhance `convertMarkdownToHtml` to wrap the entire result in a `<p>` tag and fix potential open-tag issues.

### 2. Admin Visibility & Merchant Approval
#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `getVendors` and `getKitchens` to return more details.
- Implement a combined `getMerchants` view that shows all business entities (Vendors + Kitchens) in a unified list for approval.

#### [MODIFY] [vendors.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/vendors.ejs) & [kitchens.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kitchens.ejs)
- Add columns for **Category**, **Accepts COD**, and **Commission Rate**.
- Add an "Approve" button that calls `updateMerchantKYCStatus`.

#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Link `/merchants` to the new unified view.

### 3. Financial Configurability
#### [MODIFY] [platform.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/config/platform.js)
- Refactor to prioritize values from the `settings` table, falling back to hardcoded defaults.

#### [NEW] Seed Migration
- Add `food_commission`, `groceries_commission`, and `shop_commission` to the `settings` table via a new migration.

### 4. Android Robustness
#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Ensure `DiscoveryItem` and `MerchantProfile` have sensible defaults for all new fields to prevent parsing crashes if the backend response is partial.

## Verification Plan

### Automated/Code Verification
- Verify successful Gradle build.
- Verify all backend routes load without 500 errors.

### Manual Verification
1.  **Legal Check**: Access `/legal/privacy` as an unauthenticated user. Verify it loads with the public layout.
2.  **Admin Check**: Log in as admin. Go to Partners -> Marketplace. Verify you see the Category and COD toggle status.
3.  **Approval Check**: Create a new Merchant, then use the Admin dashboard to mark them as `VERIFIED`. Verify they receive the welcome email.
4.  **Config Check**: Change the `groceries_commission` in the DB settings. Verify the change is reflected in new orders.
