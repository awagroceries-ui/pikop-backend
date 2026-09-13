# Implementation Plan - Home Screen & Marketplace Module Restructuring

This plan restructures the customer home screen to clearly separate Dispatch, Food (Kitchen), Groceries, and Shop (Marketplace) into four distinct entry points, each leading to its own dedicated flow, replacing the current "everything-at-once" architecture.

## 🔍 Diagnostic Report (Current Implementation)

I have investigated the codebase and found the following:
1. **The UI Jam (Home Screen)**: `CustomerHomeScreen.kt` currently prioritizes a single massive "Request a Delivery" (Dispatch) card. The commerce features aren't even explicitly on the home screen—they are relegated to the bottom navigation bar under "Shop & Eat".
2. **The Logic Jam (Storefront)**: The `StorefrontScreen.kt` acts as a monolithic generic screen. It fetches a generic list via `apiService.getDiscovery()`, lumping together meals, groceries, and electronics. The only distinction is a horizontal `LazyRow` of generic filter chips ("Food", "Groceries", "Electronics").
3. **The Data Layer**: The backend `commerceController.js` serves everything through `getDiscovery` using SQL category filters. Meals are distinguished via an `item_type == 'meal'` tag (added by the backend, or assumed by frontend), and products have `category`. The underlying data model allows separation via SQL filtering.

This confirms the core issue: the frontend forces a single generic flow over data that should be contextually separated.

## User Review Required

> [!IMPORTANT]
> **Distinct Screens vs. Shared Parameterized Screen**
> The prompt requests that Food, Groceries, and Shop have "their own screen". I will create three separate composable screens (`FoodStorefrontScreen`, `GroceryStorefrontScreen`, `ShopStorefrontScreen`) instead of one generic `StorefrontScreen(mode=...)`. They will share a base `DiscoveryItemCard` component to avoid duplicating the UI card itself, but the screen layout, searching, and filtering will be distinct for each. Do you approve this strict separation?

## Proposed Changes

### Part 1: Home Screen Overhaul

#### [MODIFY] [CustomerHomeScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/CustomerHomeScreen.kt)
- Remove the large "Request a Delivery" card.
- Introduce a 2x2 grid of distinct `ModuleCard` components right on the home screen:
    - **Dispatch**: Uses `DirectionsBike` or similar. Navigates to `order_quote`.
    - **Food**: Uses `Restaurant` or `Fastfood`. Navigates to `storefront_food`.
    - **Groceries**: Uses `LocalGroceryStore`. Navigates to `storefront_groceries`.
    - **Shop**: Uses `ShoppingBag`. Navigates to `storefront_shop`.
- Retain the active/incoming mission banners and secondary grid items below the primary modules.

### Part 2: Separate Storefront Screens

#### [NEW] `FoodStorefrontScreen.kt`, `GroceryStorefrontScreen.kt`, `ShopStorefrontScreen.kt`
- Create these three distinct files under `feature/commerce/`.
- Each will have its own tailored header (e.g., "Order Food" vs "Grocery Shop").
- Each will hard-enforce category logic when calling the `getDiscovery` API (e.g., Food only asks for meals; Groceries only asks for grocery categories).
- Each will have tailored sub-filters (e.g., Food might filter by cuisine type; Shop by Electronics/Fashion).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Remove the generic `storefront` route from the bottom navigation graph. Replace it with explicit routes: `storefront_food`, `storefront_groceries`, `storefront_shop`.
- Remove the "Shop & Eat" bottom navigation item to strictly enforce that users enter these flows via the Home screen grid. The bottom nav will just contain Home, Missions, Wallet, and Menu.

### Part 3: Backend Logic Refinement

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- Ensure the `getDiscovery` SQL query strictly enforces requested categories/types without cross-pollination.
- Introduce an `item_type` query parameter (e.g., `?item_type=meal` or `?item_type=product`) to firmly separate Kitchen menus from Retail inventories.

## Verification Plan

### Automated/Code Verification
- Verify `MainActivity.kt` routes successfully to the 4 independent modules.

### Manual Verification
1. Open the app as a customer. Verify the home screen shows four distinct 2x2 boxes with unique icons.
2. Verify the bottom navigation bar has been updated to remove the overlapping "Shop & Eat" tab.
3. Click "Food". Verify the screen title is specific to food and only meals appear.
4. Click "Shop". Verify NO food items appear, and the sub-filters are related to retail (Electronics, Fashion, etc.).
5. Click "Groceries". Verify only raw grocery items appear.
6. Click "Dispatch". Verify it opens the existing quote flow.