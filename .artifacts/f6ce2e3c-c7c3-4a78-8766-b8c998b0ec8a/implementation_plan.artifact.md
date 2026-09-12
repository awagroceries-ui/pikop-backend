# Implementation Plan - Pikop Commerce Phase 2: Inventory Management

This phase focuses on empowering registered merchants (Vendors and Kitchens) to manage their product catalogs and menus directly within the Pikop app.

## User Review Required

> [!IMPORTANT]
> **Unified Inventory UI:** I will implement a single, smart "Add/Edit Item" screen that adapts its fields based on whether the user is a General Vendor (Marketplace) or a Cloud Kitchen (Food).
> - **Vendors:** See fields for "Unit" (e.g., kg, pack) and "NAFDAC Number" (optional).
> - **Kitchens:** See fields for "Prep Time" and "Modifiers" (e.g., Extra Spicy).

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- **`getMerchantProfile` [NEW]**: Endpoint to return the user's active merchant profile (Vendor or Kitchen), status, and linked ID. This allows the app to know if it should show the "Registration" or "Portal" flow.
- **`getMerchantDashboard`**: Update to include basic stats like "Total Products" and "Active Orders count."

#### [MODIFY] [marketplaceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/marketplaceController.js)
- **`updateProduct` [NEW]**: Allow vendors to edit price, stock, and descriptions.
- **`deleteProduct` [NEW]**: Soft-delete or remove products from the marketplace.

#### [MODIFY] [kitchenController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/kitchenController.js)
- **`updateMenuItem` [NEW]**: Allow kitchens to edit prices, availability, and prep times.
- **`deleteMenuItem` [NEW]**: Remove items from the kitchen menu.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add endpoints for `updateProduct`, `deleteProduct`, `updateMenuItem`, and `deleteMenuItem`.
- Add `getMerchantProfile()` to fetch ownership details.

#### [NEW] [feature/merchant] [AddEditProductScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/AddEditProductScreen.kt)
- **Dynamic Form:**
    - Basic: Name, Price, Category, Description.
    - Image: Camera/Gallery integration for product photos.
    - Specialized: Unit/NAFDAC (Vendor) vs. Prep Time (Kitchen).
- **Actions:** "Create Listing" or "Save Changes."

#### [MODIFY] [feature/merchant] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Add a floating action button (FAB) "+" to open the `AddEditProductScreen`.
- Add "Edit" and "Delete" icons to product cards.
- Implement pull-to-refresh for the listings tab.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Register the `add_edit_product` route.
- Add logic to redirect "Merchant Portal" clicks to "Registration" if the user hasn't applied yet.

---

## Verification Plan

### Manual Verification
1.  **Identity Check:** Log in with a regular account. Click "Merchant Portal." Verify it redirects to the Registration form.
2.  **Creation Flow:** As an approved merchant, click "+" in the portal. Add a new product with a photo. Verify it appears in the "Listings" tab immediately.
3.  **Update Flow:** Change the price of a listed item. Verify the change is reflected in the UI and database.
4.  **Specialization:** Verify that a "Kitchen" merchant sees the "Prep Time" field while a "Vendor" merchant sees the "Unit" field.
