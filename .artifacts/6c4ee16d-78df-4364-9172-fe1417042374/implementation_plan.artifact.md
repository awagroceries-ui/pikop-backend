# Implementation Plan - Merchant Module Optimization & Link Restoration

This plan addresses the "inactive" state of the Merchant Portal and optimizes the module for product management.

## User Review Required

> [!IMPORTANT]
> **Account Menu Redesign**
> I am repurposing the "Merchant Portal" button in the Account screen to be context-aware. If the user is already a Merchant, it will be labeled "Manage My Shop" and link to their active dashboard.

## Proposed Changes

### Android Frontend (Compose)

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- Update the "Merchant Portal" button to dynamically change label and icon based on the `userRole`.
- **Label**: "Add Merchant Profile" (if Customer) vs "Manage My Shop" (if Merchant).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Update the `MerchantAppScaffold` navigation lambdas.
- Ensure `onNavigateToMerchant` links to the "dashboard" tab within the nested nav instead of being empty.

#### [MODIFY] [MerchantAppScaffold.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantAppScaffold.kt)
- Implement a `navigateToTab(route)` helper and pass it to the nested `AccountScreen`.
- This ensures that clicking "Manage My Shop" inside the Merchant "Store" tab actually switches the bottom navigation to the "Dashboard" or "Inventory" tab.

### Backend (Node.js)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Update `getSellerDashboard` to include more descriptive error logging if a user attempts to access it without an active business record.

## Verification Plan

### Manual Verification
1.  **Customer Role**: Log in as a Customer. Go to Account. Click "Merchant Portal". Verify it correctly leads to the Business Setup flow.
2.  **Merchant Role**: Log in as a Merchant. Go to the "Store" tab (Account screen). Click "Manage My Shop". Verify it switches the bottom nav to the Dashboard tab.
3.  **Product Creation**: In the Merchant Portal -> Listings tab, click "+" (Floating Action Button). Create a new product with a photo. Verify it saves and appears in the list.
4.  **Sales Tracking**: Place an order from a Customer account. Check the Merchant's "My Sales" tab. Verify the order appears instantly via Socket.io.
