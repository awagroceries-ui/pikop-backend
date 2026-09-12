# Implementation Plan - Merchant Growth: In-App Bulk Dispatch

This module empowers high-volume business users to create and manage large batches of delivery missions directly from the mobile app, significantly increasing operational efficiency for merchants.

## User Review Required

> [!IMPORTANT]
> **Single Payment for Batches:** To ensure a smooth experience, merchants will be able to pay for an entire batch of missions in one go. If their wallet balance is insufficient, they will be prompted to top up the exact total amount required for the batch.
>
> **Background Processing:** Large batches (e.g., 50+ orders) will be processed in the background. The app will show a live progress bar as each mission is activated and broadcasted to fulfillers.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- **`createBulkOrdersSession` [NEW]**: A new endpoint that works with `authenticateToken` (User Session) rather than an API key.
    - It will validate that the user owns a merchant account.
    - It will calculate the total cost for all missions in the batch.
    - It will automatically debit the merchant's wallet and create the missions.

#### [MODIFY] [merchantRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/merchantRoutes.js)
- Register `POST /api/v1/merchants/orders/bulk-session`.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `createBulkOrders(request: BulkOrderRequest): Map<String, Any>`.
- Define `BulkOrderRequest` and `BulkOrderMission` data classes.

#### [NEW] [feature/merchant] [BulkDispatchScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/BulkDispatchScreen.kt)
- **Draft List:** A list view where merchants can add delivery rows (Recipient Name, Phone, Address, Description).
- **Location Picker:** Reuse the `MapAddressSearchScreen` for each row's delivery point.
- **Batch Summary:** Displays the total mission count and total cost.
- **Action:** "Pay & Dispatch Batch."

#### [MODIFY] [feature/merchant] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Update the "Bulk" tab to include a **"Create New Batch"** button.
- Implement live progress monitoring for active batches using the `processed_orders` vs `total_orders` stats.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Register the `bulk_dispatch` route.

---

## Verification Plan

### Manual Verification
1.  **Batch Creation:** Create a batch with 3 missions. Verify the total price is the sum of all individual mission fares.
2.  **Wallet Integration:** Attempt to dispatch with ₦0 balance. Verify the app redirects to the Wallet top-up screen.
3.  **Activation:** Dispatch a valid batch. Verify all 3 missions appear in the "History" tab and are broadcasted to nearby agents.
4.  **Progress Tracking:** Watch the "Bulk" tab in the portal. Verify the progress bar moves from 0% to 100% as the batch processes.
