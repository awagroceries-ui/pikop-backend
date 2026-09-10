# Implementation Plan - Comprehensive Merchant & Seller Portal

This plan transforms the currently empty "Merchant Portal" into a comprehensive "Seller Center" that surfaces all selling activity, including Secure Pay sales, marketplace listings, and bulk mission batches.

## Problem Description
1.  **Limited Scope:** The current Merchant Portal only queries for "Bulk Mission Batches". Since most users are not high-volume programmatic merchants, the screen appears empty.
2.  **Missing Seller Data:** Activity from Secure Pay (where a user acts as a seller) and Marketplace listings are not shown anywhere for the customer, leading to a "data gap".
3.  **UI Feedback:** The portal lacks encouraging empty states or a way to distinguish between "No data yet" and "Failed to load".

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- **`getSellerDashboard` [NEW]**: A unified endpoint that aggregates:
    - **My Sales:** Missions where `seller_id = current_user_id`.
    - **My Products:** Items listed in the marketplace (via `vendors` table).
    - **Bulk Batches:** Existing order batches.
- **`getMyBatches`**: Deprecate or keep for legacy, but pivot the app to the new unified dashboard.

#### [MODIFY] [merchantRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/merchantRoutes.js)
- Register `GET /api/v1/merchants/dashboard` endpoint.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `getMerchantDashboard()` method and its corresponding data models (`MerchantDashboardResponse`, `Product`, etc.).

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- **Multi-Tab Interface:** Add tabs for **"Sales"**, **"Listings"**, and **"Bulk Batches"**.
- **Sales Tab:** Display a list of orders where the user is the seller. Show status (Held, Released, Disputed).
- **Listings Tab:** Show items the user has added to the marketplace.
- **Enhanced Empty States:** Add helpful illustrations/messages like "You haven't sold anything yet. Use Secure Pay to sell items safely!" or "Start selling on the Marketplace!"
- **Loading/Error States:** Clearly show a full-screen error with a "Retry" button if the API fails.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **The "Active Seller" Test:** Create a Secure Pay mission where the test user is the seller. Verify the mission appears in the "Sales" tab of the Merchant Portal.
2.  **The "Vendor" Test:** Register as a vendor and add a product. Verify the product appears in the "Listings" tab.
3.  **Empty State Test:** Log in with a brand new account. Verify the portal shows friendly "Get Started" messages instead of a blank screen.
4.  **Error Handling:** Temporarily disable the network and verify the "Failed to load / Retry" UI appears.
