# Implementation Plan - Order Queuing for Fulfillers

Enable fulfillers to claim a second mission that automatically activates once their current delivery is completed. This creates a "back-to-back" workflow without concurrent routing.

## User Review Required

> [!IMPORTANT]
> - **Strict Eligibility**: Fulfillers can only browse and claim a queued order *after* they have successfully picked up their current item (Status: `PICKED_UP`).
> - **Proximity Filter**: Candidates are filtered to have a pickup point within **3km** of the current order's delivery destination.
> - **Hard Limit**: Fulfillers are limited to exactly **ONE** queued order at a time.
> - **Auto-Activation**: The queued order will automatically transition to `MATCHED` and become the new active mission the moment the current order is marked as `DELIVERED`.

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1722960000000_order_queuing.js`
- **Orders**: Add `queued_for_fulfiller_id` (integer, references fulfillers).
- **Status**: Add logic support for `QUEUED` status.

---

### 2. Backend Logic (Lifecycle)

#### [MODIFY] `orderController.js`
- **New Endpoint**: `GET /api/v1/fulfillers/me/queue-candidates`
    - Logic: Find `SEARCHING` orders within 3km of the fulfiller's *active* delivery point.
- **New Endpoint**: `POST /api/v1/orders/:orderId/queue/claim`
    - Logic: Atomically claim an order into the `QUEUED` state.
- **Update `verifyDelivery`**:
    - After an order is marked `DELIVERED`, check for a matching `QUEUED` order.
    - If found, promote it to `MATCHED`, assign the `fulfiller_id`, and emit a socket event to the User.

#### [MODIFY] `fulfillerController.js`
- Ensure `getProfile` or dashboard includes information about current queued missions.

---

### 3. Fulfiller UI (Android)

#### [MODIFY] `ActiveOrderScreen.kt`
- **Queue Section**: When the status is `PICKED_UP`, show a "Queue Your Next Mission" section.
- **Preview Card**: Reuse `IncomingOfferComponent` to show the item photo and masked summary of queue candidates.
- **"Up Next" Display**: If an order is already queued, show a distinct "Up Next" summary card at the bottom of the screen.

#### [MODIFY] `ApiService.kt`
- Add `getQueueCandidates` and `claimQueueOrder` endpoints.

---

### 4. User Experience (Android)

- Users with a `QUEUED` order will see their status as "Fulfiller Assigned," but real-time tracking will only activate once the driver completes their current mission and starts moving toward them.

## Verification Plan

### Manual Verification
1.  **Pickup Guardrail**: Verify that a fulfiller *cannot* see queue candidates before they have verified the pickup code for their first order.
2.  **Radius Test**: Ensure only orders near the *destination* of the current mission appear in the queue list.
3.  **Auto-Transition**: Complete a delivery and verify that the app instantly switches to the next mission's pickup instructions.
4.  **Conflict Check**: Verify that a fulfiller cannot claim a second queued order.
