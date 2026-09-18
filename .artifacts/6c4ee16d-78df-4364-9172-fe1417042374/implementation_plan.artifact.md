# Implementation Plan - Fix Merchant Product Creation

This plan addresses the issue where Merchants cannot create products, primarily due to legacy account residue and incomplete verification status sync.

## Findings

1.  **Legacy Account Gap**: Merchants created via the old "Join as Merchant" path have the `MERCHANT` role but lack a `vendors` or `kitchens` profile record.
2.  **UI Gating**: The "Add Item" button is hidden if no profile record exists, effectively locking legacy merchants in a dead state.
3.  **Status Sync**: While approval updates the database, the backend was returning a limited set of fields, and the UI wasn't explicitly handling the "Pending Verification" state within the Seller Center.

## Proposed Changes

### 1. Backend Fixes (Node.js)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- **`getMerchantProfile`**: Update SQL to return all fields (`SELECT *`) from `vendors` and `kitchens`.

#### [MODIFY] [marketplaceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/marketplaceController.js) & [kitchenController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/kitchenController.js)
- **`addProduct` / `addMenuItem`**: Add a check to ensure the merchant's status is `active` before allowing a new listing to be saved.

### 2. Android UI Fixes (Compose)

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- Update "Manage My Shop" click logic: Always verify the existence of a merchant profile record, even for users with the `MERCHANT` role. If missing, route to `onNavigateToMerchantRegistration`.

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- **Handle Missing Profile**: If `merchantProfile` is null but the user is a `MERCHANT`, show a "Complete Business Setup" CTA.
- **Verification Banner**: Show a "Verification Pending" notice if `status != 'active'`.
- **Gate "Add Item"**: Only show the FAB if `status == 'active'`.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Ensure `MerchantProfile` DTO matches all fields returned by the backend.

## Verification Plan

### Manual Verification
1.  **Legacy Merchant Fix**: Take an account with role `MERCHANT` but no vendor/kitchen record. Click "Manage My Shop" in Account. Verify it routes to the Business Setup screen.
2.  **Pending State**: Approve a merchant but set status to `pending_business_verification`. Verify the Seller Center shows a "Verification Pending" banner and hides the "Add Item" button.
3.  **End-to-End Creation**: Approve a merchant fully (`active`). Verify they can click "Add Item", fill the form, and the product appears in their "Items" tab.
