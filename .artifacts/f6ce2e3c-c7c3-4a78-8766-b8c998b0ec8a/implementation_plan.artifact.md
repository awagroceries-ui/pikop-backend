# Implementation Plan - Pikop Commerce (Marketplace & Kitchens)

This module transforms Pikop from a courier service into a full-scale commerce ecosystem, allowing businesses to sell products/food and customers to purchase them directly within the app.

## User Review Required

> [!IMPORTANT]
> **Unified Checkout:** When a user buys a product from the marketplace, the system will automatically calculate the delivery fare from the Vendor's location to the Customer's location. The user will pay for both the **item** and the **delivery** in a single Paystack transaction.
>
> **Merchant Payouts:** Funds for items sold will be held in the platform escrow and released to the Merchant's wallet only after the customer confirms receipt in the app.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [marketplaceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/marketplaceController.js)
#### [MODIFY] [kitchenController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/kitchenController.js)
- **Profile Integrity:** Ensure `getVendorDetails` and `getKitchenDetails` return the owner's status and linked `pickup_address` for delivery calculations.
- **Photo Storage:** Ensure product/menu item photo uploads are handled via the unified `/uploads` service.

#### [NEW] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- **Storefront Search:** Implement a unified search endpoint that queries both Products and Menu Items based on proximity to the user.

---

### Android App

#### [NEW] [feature/merchant] [MerchantRegistrationScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantRegistrationScreen.kt)
- **Form:** Business Name, CAC Number, Contact Email, Bank Account (Name/Number/Code).
- **Type Selection:** Toggle between "Marketplace Vendor" (Items) or "Cloud Kitchen" (Food).
- **Location:** Integrated address picker to set the business's permanent pickup point.

#### [NEW] [feature/merchant] [ProductManagementScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/ProductManagementScreen.kt)
- Interface for adding items/meals with price, description, and photo capture.

#### [NEW] [feature/order] [StorefrontScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/StorefrontScreen.kt)
- **Discovery Hub:** Categories (Electronics, Fashion, Food, etc.).
- **Listings:** Scrollable grid of products and nearby kitchens.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Register routes for `merchant_registration`, `product_mgmt`, and `storefront`.
- Add a **"Shop & Eat"** tab to the main navigation bar.

---

## Verification Plan

### Manual Verification
1.  **Onboarding:** Register a test business as a "Kitchen." Verify the application appears as "Pending" in the database.
2.  **Listing:** Add a "Jollof Rice" menu item with a photo. Verify it appears in the database and is linked to the kitchen.
3.  **Discovery:** As a customer, open the Storefront. Verify the "Jollof Rice" item is visible and shows the correct price and vendor.
4.  **Integrated Order:** Select an item -> Checkout. Verify the total price includes the item cost + delivery fee to the user's current location.
