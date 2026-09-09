# Implementation Plan - Fix Wallet Balance, Activity History & Order Creation

This plan addresses the persistent issues where fulfiller history is empty, wallet balances are ₦0, and customer top-ups don't reflect.

## Problem Description
1.  **Broken Order Creation:** The `INSERT` statements for orders on the backend are using incorrect parameter indices and missing columns, causing order creation to fail or save incomplete data.
2.  **Fulfiller Earnings:** Payouts for free missions are not being calculated from the original price due to the broken insert logic.
3.  **Stale UI Display:** The Android wallet and history screens load data only once on launch, so they don't reflect new transactions or status changes without an app restart.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Fix the `createOrder` (Manual Fallback) SQL query to correctly map all 34 parameters, including the new `original_delivery_fee` and `original_total_fare` columns.
- Ensure `pickup_code` and `delivery_code` are generated and saved correctly alongside their hashes.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Fix the `activatePaidMission` SQL query to align with the 34-parameter V3 schema.
- Ensure `original_` columns are populated from the quote for correct payout calculation.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Ensure `processMissionSettlement` uses `original_delivery_fee` as the primary source for fulfiller 75% calculation.

---

### Android App

#### [MODIFY] [WalletScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/wallet/WalletScreen.kt)
- Wrap the main content in a `PullToRefreshBox` (or a similar mechanism) to allow manual data refresh.
- Add a `LifecycleEventObserver` to trigger a refresh whenever the user returns to the wallet screen.

#### [MODIFY] [FulfillerOrdersScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerOrdersScreen.kt)
- Add a `LifecycleEventObserver` to refresh the mission history whenever the screen gains focus.

---

## Verification Plan

### Automated Tests
- Syntax check backend files: `node -c ...`.
- Run Android build: `./gradlew assembleDebug`.

### Manual Verification
1.  **Order Persistence:** Create a mission (free or paid) and verify the record in the `orders` table has all fields populated (`item_price`, `original_delivery_fee`, `pickup_code`, etc.).
2.  **Wallet Refresh:** Perform a wallet top-up, return to the app, and verify the balance updates without a restart.
3.  **Fulfiller History:** Complete a mission and verify it appears in the fulfiller's history tab with calculated earnings immediately.
