# Implementation Plan - Updated Strict Policy Enforcement

This plan refines the platform's financial policies regarding cancellations and returns to protect fulfillers and ensure operational sustainability.

## User Review Required

> [!IMPORTANT]
> **Policy Adjustments:**
> 1.  **Cancellation Fee:** If a user cancels *after* an agent is matched but *before* pickup, a **25% fee** of the original fare will be charged.
> 2.  **No Cancellation After Pickup:** Once an item is marked as "Picked Up," the mission can no longer be cancelled by the user.
> 3.  **Return Charge:** If a return is initiated (e.g., due to recipient absence), the charge is now **75% of the original fare** (increased from 50%).

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`cancelOrder`**:
    - Check if the mission status is `PICKED_UP` or later. If so, reject the cancellation with a 403 error.
    - Check if `fulfiller_id` is assigned.
    - If assigned and status is `MATCHED`/`ACCEPTED`, calculate a **25% penalty** of the `total_fare`.
    - Use `walletService` to deduct this penalty from the user's wallet.
- **`initiateReturn`**:
    - Update the `returnFare` calculation to use **0.75** (75%) multiplier instead of 0.5.

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- Update the terms content to explicitly state:
    - 25% cancellation fee after agent match (pre-pickup).
    - No cancellations allowed after pickup.
    - 75% return fee for failed deliveries.
- Add `getLegalConfig` endpoint to serve these terms to the app.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Ensure `recordEntry` correctly logs the `CANCELLATION_PENALTY` purpose.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `getLegalConfig(): Map<String, String>` endpoint.

#### [MODIFY] [TermsScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/TermsScreen.kt)
- Refactor to load and render HTML content from the server for consistent policy display.

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Update the "Cancel Delivery" button visibility/logic to reflect the "No cancellation after pickup" rule.

---

## Verification Plan

### Manual Verification
1.  **Return Charge Test:** Fail a delivery -> Initiate return. Verify the return mission price is 75% of the original.
2.  **Matched Cancellation Test:** Accept mission as agent -> Cancel as user (before pickup). Verify user wallet is charged 25% of the fare.
3.  **Post-Pickup Cancellation Test:** Verify the "Cancel" button disappears or returns an error message once the mission is in `PICKED_UP` status.
4.  **Legal Sync:** Verify the app's Terms & Conditions screen shows the updated 25%/75% figures instantly.
