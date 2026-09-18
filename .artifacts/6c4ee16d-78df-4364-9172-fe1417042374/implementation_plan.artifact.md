# Implementation Plan - Scheduled/Future-Dated Orders

This plan enables customers to schedule their deliveries or marketplace orders for a specific future date and time.

## Proposed Changes

### 1. Backend Infrastructure

#### [MODIFY] [scheduledOrderJob.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/jobs/scheduledOrderJob.js)
- Update activation logic: Trigger dispatch matching **30 minutes prior** to `scheduled_at`.
- Ensure it respects the Africa/Lagos (WAT) timezone.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Validation**:
    - Max scheduling limit: 7 days into the future.
    - Security Window: If the order requires a Foot Agent, Cyclist, or Rider, the scheduled time must be between **6:00 AM and 6:00 PM**.
- Handle rescheduling: Allow users to update `scheduled_at` while status is still `SCHEDULED`.

#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- Support `scheduled_at` field.
- **Validation**: Ensure the requested time falls within the Merchant's `operating_hours`.
- Pass `scheduled_at` through to the created order record.

#### [MODIFY] [marketplaceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/marketplaceController.js)
- Include `operating_hours` in the `discovery` and `vendor_details` responses.

### 2. Android App Integration

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `scheduled_at` to `CommerceOrderRequest`.
- Add `operating_hours` (Map or structured object) to `DiscoveryItem`.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Add a "Scheduling" section with a toggle: **"Deliver Now"** vs **"Schedule for Later"**.
- Implement a Date and Time picker using Material 3 `DatePickerDialog` and `TimePickerDialog`.
- Validate that the chosen time is at least 1 hour from now and within the 6 AM - 6 PM security window for riders.

#### [MODIFY] [CommerceCheckoutScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/CommerceCheckoutScreen.kt)
- Add the same Scheduling section.
- **Enhanced Validation**: Check chosen time against the merchant's operating hours (surfaced in `DiscoveryItem`).

## User Review Required

> [!IMPORTANT]
> **Activation Lead Time**
> I am setting the background job to activate missions **30 minutes before** the scheduled time. This ensures a driver is matched and moving *by* the time the customer expects the pickup.

> [!NOTE]
> **Security Enforcement**
> Scheduled times outside the 6 AM - 6 PM window will be automatically restricted to "Vehicle Only" (Drivers) or blocked if no drivers are available, consistent with our night-dispatch safety policy.

## Verification Plan

### Manual Verification
1.  **Near-Future Test**: Schedule a delivery for 45 minutes from now. Verify it enters `SCHEDULED` status. Wait 15 minutes and verify the background job moves it to `SEARCHING`.
2.  **Merchant Hours Test**: Attempt to schedule a food order for 11:00 PM for a kitchen that closes at 9:00 PM. Verify the UI/API blocks the request with a helpful error.
3.  **Cancellation Test**: Cancel a `SCHEDULED` order. Verify immediate refund to wallet and that the order is never dispatched.
