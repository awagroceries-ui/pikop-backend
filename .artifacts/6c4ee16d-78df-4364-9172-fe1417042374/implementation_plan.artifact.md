# Implementation Plan - Merchant Module Optimization & Role Sync

This plan fixes the issues blocking Merchants from managing their shops and ensures a seamless transition after business approval.

## 🔍 Diagnostic Summary
1.  **Approval/Role Disconnect**: When an Admin approves a business, the user's role in the `users` table remains `CUSTOMER`. This prevents the app from switching to the dedicated `Merchant Console` view.
2.  **Dashboard Data Gap**: The `getSellerDashboard` API only fetches `products` (for Vendors) and completely ignores `menu_items` (for Kitchens). This results in an empty "Listings" tab for food merchants.
3.  **App Cache Bug**: The profile sync loop in the Android app incorrectly saves the *old* role from local storage back into the token manager, ignoring any role upgrades performed on the server.
4.  **Inactive Button**: The "Merchant Portal" button in the Account menu feels inactive because its internal logic was not correctly handling role-aware navigation in the `MerchantAppScaffold`.

## Proposed Changes

### 1. Backend Fixes (Node.js)

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `updateMerchantKYCStatus` to set `users.role = 'MERCHANT'` when a business is verified.

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Update `getSellerDashboard` to fetch `menu_items` if the user owns a kitchen.
- Consolidate `products` and `menu_items` into a unified `listings` array in the dashboard response for easier UI consumption.

---

### 2. Android App Fixes (Compose)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Fix the Profile Sync loop: ensure `role = profile.role ?: "CUSTOMER"` is used when saving to `TokenManager`.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `MerchantDashboardData` to include `menu_items` or a unified `listings` field.

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Update `ListingsTabContent` to handle the unified listings data.
- Ensure the "Add Item" FAB works for both `vendor` and `kitchen` types.

---

## Verification Plan

### Manual Verification
1.  **Approval Flow**: Register a new Merchant account (Step 1 & 2). Approve the merchant in the Admin Portal. Verify that the app's next profile sync (within 60s) automatically switches the UI to the Merchant Console.
2.  **Kitchen Dashboard**: Log in as a Kitchen merchant. Add a meal. Verify it appears in the "Listings" tab.
3.  **Vendor Dashboard**: Log in as a Vendor merchant. Add a product. Verify it appears in the "Listings" tab.
4.  **Navigation**: Click "Manage My Shop" from the Account tab inside the Merchant view. Verify it correctly switches to the Dashboard tab.
