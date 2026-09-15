# Implementation Plan - Merchant Operating Hours & Daylight Dispatch Security

This plan implements (1) configurable merchant operating hours and (2) a security rule restricting Foot Agents, Cyclists, and Riders to daylight hours (6 AM - 6 PM WAT), falling back to next-day scheduling for night missions without drivers.

## Proposed Changes

### 1. Database Schema & Settings
- **[NEW] Migration**:
    - Add `operating_hours` (JSONB) to `vendors` and `kitchens` tables.
    - Add `scheduled_at` (TIMESTAMP) to `orders` table.
    - Add `daylight_dispatch_start` and `daylight_dispatch_end` to `settings` table (seeded with '06:00' and '18:00').

### 2. Backend Logic: Merchant Operating Hours
- **[MODIFY] `merchantController.js`**: Update `updateMerchantSettings` to handle `operating_hours` updates.
- **[MODIFY] `commerceController.js`**:
    - In `getDiscovery`, calculate `is_open` and `next_open_time` based on the merchant's `operating_hours` and current WAT time.
    - In `initializeCommerceOrder`, block orders if the target merchant is currently closed.

### 3. Backend Logic: Daylight Dispatch Security
- **[MODIFY] `dispatchService.js`**:
    - Update `findNearbyFulfillers` to fetch daylight window from settings.
    - If current WAT is outside this window, exclude `rider` and `agent` (Foot Agent/Cyclist) from the `primary_class` filter.
- **[MODIFY] `orderController.js`**:
    - In `getQuote`, check if it's restricted time and return `restricted_dispatch: true` + `drivers_count` (available Drivers only).
    - In `createOrder`, handle `status = 'SCHEDULED'` if `scheduled_at` is provided in the request.
- **[NEW] Job**: `scheduledOrderJob.js` to activate scheduled missions at 6 AM WAT.

### 4. Android App (Compose)
- **[MODIFY] `ApiService.kt`**: Add `is_open`, `next_open_time` to `DiscoveryItem` and `restricted_dispatch`, `drivers_count` to `QuoteResponse`.
- **[MODIFY] `StorefrontScreen.kt`**: Update `DiscoveryItemCard` to show a "CLOSED" overlay and disable clicks if `is_open` is false.
- **[MODIFY] `OrderQuoteScreen.kt`**:
    - If `restricted_dispatch` is true and `drivers_count == 0`, show a dialog: *"No drivers available right now — schedule this for tomorrow starting 6:00am?"*.
    - If user accepts, set `scheduled_at` (next day 06:00 WAT) in the order request.

## User Review Required

> [!IMPORTANT]
> **Admin Configurability**
> I am making the 6 AM - 6 PM security window admin-configurable via the Global Settings page, consistent with other platform rules.

> [!NOTE]
> **Timezone Handling**
> I will use a central `time.js` utility on the backend to ensure all checks use Nigeria (WAT) time, regardless of the server's system time.

## Verification Plan

### Automated/Code Verification
- Verify `dispatchService.js` correctly filters categories based on time.
- Verify `commerceController.js` correctly evaluates `is_open`.

### Manual Verification
1.  **Merchant Hours**: Set a merchant's hours to close at 5 PM. Try to order at 6 PM. Verify it shows "Closed" and blocks checkout.
2.  **Daylight Dispatch**: Request a dispatch at 7 PM. Verify Foot Agents/Riders are not notified.
3.  **Scheduling**: Request a dispatch at 7 PM when no Drivers are online. Verify the "Schedule for tomorrow?" prompt appears and works.
4.  **Mid-mission**: Start a mission as a Rider at 5:55 PM. Verify it is NOT interrupted at 6:00 PM.
