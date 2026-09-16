# Implementation Plan - Remove Legacy Merchant Entry Points

This plan removes the legacy "Join as Merchant" entry points and ensures all merchant onboarding follows the proper KYC/KYB verification flow.

## Proposed Changes

### Backend (Node.js)

#### [MODIFY] [marketplaceRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/marketplaceRoutes.js)
- Remove the `POST /vendors/register` route.

#### [MODIFY] [kitchenRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/kitchenRoutes.js)
- Remove the `POST /register` route.

#### [MODIFY] [marketplaceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/marketplaceController.js)
- Delete the `registerVendor` function.

#### [MODIFY] [kitchenController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/kitchenController.js)
- Delete the `registerKitchen` function.

---

### Android Frontend (Compose)

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- **Remove** the `AccountOption` for "Join as a Merchant".
- **Update** the `AccountOption` for "Merchant Portal":
    - When no profile exists, change the label to "Add Merchant Profile".
    - Repurpose the click action to lead to the proper onboarding step (`merchant_business_setup`).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- **Update** the `account` route navigation: Map `onNavigateToMerchantRegistration` to navigate to `merchant_business_setup`.
- **Remove** the legacy `merchant_registration` route definition.

#### [DELETE] [MerchantRegistrationScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantRegistrationScreen.kt)
- Remove the old screen file completely.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- **Remove** the legacy methods: `registerVendor` and `registerKitchen`.
- **Remove** the associated request data classes: `VendorRegistrationRequest` and `KitchenRegistrationRequest`.

## Auditing & Security Note

> [!WARNING]
> **Legacy Unverified Accounts**
> Merchants created via the old flow are stored with status `'pending'`. The new flow uses `'pending_business_verification'`.
> The Admin Dashboard's "Verification Queue" already lists all non-active merchants. Any record with the status `'pending'` found in that list was created via the bypass and should be manually reviewed or suspended by an admin.

## Verification Plan

### Automated/Code Verification
- Verify successful Gradle build after removing legacy files and methods.
- Verify backend routes no longer exist.

### Manual Verification
1.  **Settings UI**: Confirm "Join as a Merchant" is gone from Customer and Fulfiller settings.
2.  **Add Role Flow**: As a Customer, tap "Add Merchant Profile". Confirm it opens the new `Business Verification` screen (Step 2), not the old registration form.
3.  **Bypass Check**: Attempt to call the old registration endpoints via postman/cURL. Confirm they return 404.
