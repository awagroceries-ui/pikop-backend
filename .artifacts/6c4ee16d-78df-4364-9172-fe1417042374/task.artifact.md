# Task Checklist: Dedicated Merchant Module Separation

- `[/]` **Part 1: Separate Merchant Navigation**
  - `[ ]` Create `MerchantAppScaffold.kt` with a dedicated bottom navigation bar.
  - `[ ]` Update `MainActivity.kt` `MainAppScaffold` routing to direct `MERCHANT` role to the new scaffold.
- `[ ]` **Part 2: Refactoring Merchant Portal Components**
  - `[ ]` Adapt `MerchantPortalScreen.kt` content into distinct tabs (Dashboard, Inventory, Orders, Wallet).
- `[ ]` **Part 3: Backend Order Management**
  - `[ ]` Add/verify `getIncomingOrders` endpoint for merchants.
  - `[ ]` Add/verify `updateOrderStatus` endpoint for merchants (`PREPARING`, `READY_FOR_PICKUP`).
- `[ ]` **Part 4: Verification & Git**
  - `[ ]` Compile and test navigation boundaries.
  - `[ ]` Git commit and push changes.