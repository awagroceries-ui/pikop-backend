# Task Checklist: Marketplace Module Restructuring

- `[/]` **Part 1: Home Screen Overhaul**
  - `[ ]` Modify `CustomerHomeScreen.kt` to remove the large "Request a Delivery" card.
  - `[ ]` Introduce a 2x2 grid of distinct `ModuleCard` components (Dispatch, Food, Groceries, Shop).
- `[ ]` **Part 2: Dedicated Storefront Screens**
  - `[ ]` Create `FoodStorefrontScreen.kt`.
  - `[ ]` Create `GroceryStorefrontScreen.kt`.
  - `[ ]` Create `ShopStorefrontScreen.kt`.
- `[ ]` **Part 3: Navigation Updates**
  - `[ ]` Update `MainActivity.kt` to register the new explicit routes (`storefront_food`, `storefront_groceries`, `storefront_shop`).
  - `[ ]` Remove the "Shop & Eat" generic bottom navigation item.
- `[ ]` **Part 4: Backend Updates**
  - `[ ]` Modify `commerceController.js` `getDiscovery` endpoint to strictly enforce `item_type` query parameters if not already present.
- `[ ]` **Part 5: Verification**
  - `[ ]` Build and test the Android client.
  - `[ ]` Verify visually that the home screen has the 4 boxes and bottom nav is updated.
  - `[ ]` Verify navigation works without overlapping data.
  - `[ ]` Git commit and push changes.