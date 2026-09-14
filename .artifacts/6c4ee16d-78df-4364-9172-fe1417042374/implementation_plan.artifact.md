# Implementation Plan: Dedicated Merchant Module Separation

## 🔍 Diagnostic Report

1. **Current Entanglement**: The `MerchantPortalScreen` is heavily entangled inside the *Customer/General* application graph.
    - Currently, all users (Customer, Fulfiller, Merchant) log in and are routed to the `MainAppScaffold` in `MainActivity.kt`.
    - Inside `MainAppScaffold`, `userRole` conditionally renders either the `CustomerHomeScreen` or the `FulfillerDashboardScreen`.
    - **The Problem**: There is *no* dedicated home screen for Merchants. A merchant currently lands on the `CustomerHomeScreen` (because their role isn't Fulfiller), and has to navigate to the "Menu" (AccountScreen) to find a sub-menu button called "Merchant Portal" to actually manage their business.
2. **Current Handoff Points**:
    - **Customer -> Merchant**: Customers use `FoodStorefrontScreen` / `GroceryStorefrontScreen` to query public data. Perfect.
    - **Merchant -> Fulfiller**: `MerchantPortalScreen` allows creating bulk orders.
3. **Target Architecture**: The prompt specifies that a Merchant must *default* into their own Merchant experience on login, bypassing the customer home screen entirely, unless they deliberately switch apps/roles.

## User Review Required

> [!IMPORTANT]
> **Module Architecture: In-App Dedicated Flow**
> The most efficient way to achieve this without forcing users to download a completely separate APK is to implement the "cleanly separated in-app section" as permitted by the prompt.
>
> I will modify `MainAppScaffold` in `MainActivity.kt`. Currently, it assumes the user is either a Fulfiller or a Customer. I will add a third root branch: if `userRole == "MERCHANT"`, the entire `Scaffold` will swap out to a dedicated `MerchantAppScaffold`. This scaffold will have its own bottom navigation bar (e.g., Dashboard, Products, Orders, Wallet) and trap the Merchant inside their business operations context, totally separate from the customer parcel-sending UI.
>
> Do you approve of this approach?

## Proposed Changes

### Part 1: Separate Merchant Navigation

#### [NEW] [MerchantAppScaffold.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantAppScaffold.kt)
- Create a dedicated root UI for Merchants.
- Bottom Navigation:
    - **Dashboard**: High-level stats, quick actions (Create Batch).
    - **Inventory**: The current product/menu management list.
    - **Orders**: Incoming order management (view, accept/reject, mark ready).
    - **Wallet**: Earnings/escrow.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- In the `composable("main")` block, update the routing logic:
  - If `userRole == "FULFILLER"`, show `FulfillerAppScaffold` (or existing Fulfiller logic).
  - If `userRole == "MERCHANT"`, show `MerchantAppScaffold`.
  - Else, show the existing `MainAppScaffold` (Customer).

### Part 2: Refactoring Merchant Portal Components

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Break down the current monolithic `MerchantPortalScreen` (which relies on tabs) into individual top-level screens that slot into the new `MerchantAppScaffold` bottom nav tabs.
- Ensure the "Orders" tab connects to the existing order state machine (using an endpoint to fetch merchant-specific orders, likely requiring a quick backend check/addition).

### Part 3: Backend Order Management (Handoffs)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js) (If needed)
- Ensure there is an endpoint for Merchants to fetch *incoming* customer orders.
- Ensure there is an endpoint for Merchants to update order status (`PAYMENT_CAPTURED` -> `PREPARING` -> `READY_FOR_PICKUP`).

## Verification Plan

### Manual Verification
1. Log in as a `CUSTOMER`. Verify I land on the Customer Home Screen (Dispatch, Food, Groceries).
2. Log in as a `MERCHANT`. Verify I land directly on the `MerchantAppScaffold` (Dashboard, Inventory, Orders).
3. Verify Customer screens (like "Request Delivery" or storefronts) do not bleed into the Merchant UI.
4. Verify the Merchant UI provides buttons to mark an order "Ready for Pickup", properly handing off the state machine to the Fulfiller app.