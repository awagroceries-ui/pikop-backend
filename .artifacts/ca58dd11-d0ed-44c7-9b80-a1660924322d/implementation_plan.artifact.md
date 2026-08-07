# Implementation Plan - Live Order Status History

Populate the tracking timeline with real-time data from the backend by implementing an order status history tracking system.

## User Review Required

> [!IMPORTANT]
> - **Historical Data**: Once this is implemented, only *new* status changes will appear in the timeline. Existing orders will have empty histories until updated.
> - **Polling vs Sockets**: I will fetch the history once when the `TrackOrderScreen` opens. For subsequent updates, the app will listen for a `status_changed` socket event to refresh the timeline.

## Proposed Changes

### Backend: Tracking System

#### [NEW] `backend/migrations/1722880000000_order_status_history.js`
- Create `order_status_history` table: `id`, `order_id`, `status`, `description`, `created_at`.

#### [MODIFY] `backend/src/controllers/orderController.js`
- Implement `getOrderDetails`: Returns order data and its full status history.
- Update lifecycle methods (`createOrder`, `acceptOrder`, etc.) to insert records into `order_status_history` on every transition.
- **Socket Integration**: Emit a `status_updated` event via `socketService` whenever the order state changes.

#### [MODIFY] `backend/src/routes/orderRoutes.js`
- Expose `GET /api/v1/orders/:orderId`.

---

### Android: Live UI Integration

#### [MODIFY] `app/src/main/java/com/ng/pikop/core/network/ApiService.kt`
- Add `OrderDetailsResponse` and `StatusHistoryItem` data models.
- Add `suspend fun getOrderDetails(id: String): OrderDetailsResponse`.

#### [MODIFY] `app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt`
- Remove mock history data.
- Add a `LaunchedEffect` to fetch the real history from `ApiService` on initialization.
- Update the socket listener to trigger a re-fetch of the history when a `status_updated` event is received.

---

## Verification Plan

### Automated Tests
- Integration test: Create an order, verify it, and confirm the `order_status_history` table contains the expected records.
- API test: Verify `GET /orders/:id` returns the correct history array.

### Manual Verification
- Place an order and verify the "Order Placed" step appears in the timeline.
- Verify as a Fulfiller and confirm the "Driver Assigned" and "Picked Up" steps update in real-time on the User's tracking screen.
