# Implementation Plan - Pikop Commerce Phase 3: Customer Storefront

This phase focuses on the customer-facing "Storefront," enabling users to discover, browse, and search for products and food from nearby vendors and kitchens.

## User Review Required

> [!IMPORTANT]
> **Unified Discovery Hub:** I will implement a single "Storefront" screen that combines both Marketplace Products and Cloud Kitchen Meals into one seamless browsing experience.
> - **Proximity Sort:** Items will be automatically sorted by their distance from the user's current location (requires location permissions).
> - **Categories:** Users can filter by "Food & Meals," "Groceries," "Electronics," etc.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- **`getDiscovery` [NEW]**: Unified search and listing endpoint.
    - Queries `products` and `menu_items` tables.
    - Joins with `vendors`/`kitchens` and `addresses` to calculate distance using PostGIS.
    - Supports keyword search and category filtering.

#### [NEW] [commerceRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/commerceRoutes.js)
- Register `GET /api/v1/commerce/discovery`.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `getDiscovery(lat: Double?, lng: Double?, category: String?, query: String?): DiscoveryResponse`.
- Define `DiscoveryItem` and `DiscoveryResponse` data classes.

#### [NEW] [feature/commerce] [StorefrontScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/StorefrontScreen.kt)
- **Search Bar:** Real-time filtering of items.
- **Category Chips:** Horizontal scroll for quick filtering (e.g., Food, Electronics, Pharmacy).
- **Discovery Grid:** A vertical grid showing item cards with photo, price, vendor name, and distance.
- **Details Navigation:** Clicking an item opens a "Storefront Item Details" modal (preparing for Phase 4).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add a new **"Shop & Eat"** tab to the `MainAppScaffold` bottom navigation.
- Icon: `Icons.Default.Storefront`.

---

## Verification Plan

### Manual Verification
1.  **Unified Listing:** Ensure both general products (e.g., "Fridge") and meals (e.g., "Jollof Rice") appear in the main discovery feed.
2.  **Proximity Check:** Verify that items from vendors closer to the user appear higher in the list.
3.  **Category Filter:** Click the "Food" chip. Verify only items from "Kitchens" are visible.
4.  **Search Performance:** Search for a specific product name. Verify relevant results appear instantly.
