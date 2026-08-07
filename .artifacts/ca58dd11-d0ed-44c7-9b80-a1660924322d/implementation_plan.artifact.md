# Implementation Plan - Milestone 12: Customer Orders Dashboard

Create a central dashboard for customers to manage their delivery requests, view order history, and access real-time tracking for active deliveries.

## User Review Required

> [!NOTE]
> - **Navigation Change**: The `OrderQuoteScreen` will no longer be the primary screen after login. Instead, users will land on the **Orders Dashboard**.
> - **Real-time Status**: The dashboard will poll the order status to keep the list updated as fulfillers accept and progress orders.

## Proposed Changes

### Backend: Order Management

#### [MODIFY] `backend/src/controllers/orderController.js`
- Implement `getUserOrders`: Returns a list of all orders placed by the authenticated user, sorted by the most recent.

#### [MODIFY] `backend/src/routes/orderRoutes.js`
- Expose `GET /api/v1/orders` (Secured with `authenticateToken`).

---

### Android: UI Layer

#### [MODIFY] `app/src/main/java/com/ng/pikop/core/network/ApiService.kt`
- Add `suspend fun getUserOrders(): List<OrderDetailsResponse>`.

#### [NEW] `OrdersDashboardScreen.kt` (in `com.ng.pikop.feature.order`)
- A clean list of orders showing:
    - Order ID.
    - Status Badge (e.g., Searching, Matched, In Transit).
    - Pickup/Delivery address summary.
    - "Track" button for active orders.
- A **Floating Action Button (FAB)** to "Request a Delivery" (navigates to `OrderQuoteScreen`).

#### [MODIFY] `MainActivity.kt`
- Add the `orders_dashboard` route.
- Set `orders_dashboard` as the `startDestination` for authenticated users.
- Update navigation logic so that successful login/signup leads to the Dashboard.

---

## Verification Plan

### Automated Tests
- Integration test: Create multiple orders for a user and verify that `GET /api/v1/orders` returns them in descending chronological order.

### Manual Verification
- Log in and verify the empty state of the dashboard.
- Create a new delivery request and verify it appears in the list with the "SEARCHING" status.
- Confirm that clicking "Track" opens the correct tracking screen for that specific order.
