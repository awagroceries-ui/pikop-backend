# Implementation Plan - Advanced Incident & Dispute Engine

This plan implements a robust system for handling fleet incidents, marketplace disputes, and automated financial waivers.

## Proposed Changes

### 1. Database Schema Enhancements

#### [NEW] [incident_management migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726490000000_incident_management.js)
- **Extend `disputes` table**: Add `severity` (LOW, MEDIUM, HIGH), `incident_category`, and `is_3way_bridged` (boolean).
- **[NEW] `conversation_participants` table**: To support multi-party chat (Admin + Fulfiller + Customer).
    - `conversation_id` (UUID), `user_id` (INT), `role` (ADMIN, FULFILLER, CUSTOMER).

### 2. Backend Logic (Node.js)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update `fileIncident` and `reportProblem` to support structured categories and severity.
- Implement logic to automatically create a 3-way bridged conversation if severity is "HIGH".

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Implement `applyAutomatedWaiver(orderId, waiverType)`:
    - `CANCELLATION`: Handles the 25% penalty waiver if proof is valid.
    - `RETURN`: Handles the 75% return fee waiver if the failure was not the agent's fault.

#### [MODIFY] [supportController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/supportController.js)
- Implement `bridgeIncidentChat(orderId)`: Joins the assigned Fulfiller and Customer into a single Support Conversation.

### 3. Admin Resolution Dashboard

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Create `getDisputeResolutionCenter`: A dedicated view for admins to mediate between Customers and Merchants/Fulfillers.
- Add "Apply Waiver" and "Release/Refund" actions directly in the resolution view.

### 4. Android Frontend (Compose)

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Update `IncidentReportDialog` with structured categories: `VEHICLE_BREAKDOWN`, `SAFETY_RISK`, `RECIPIENT_UNREACHABLE`, `ACCIDENT`.

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Update `SecurePayDisputeDialog` with structured categories: `INCORRECT_ITEM`, `ITEM_DAMAGED`, `DELAYED_DELIVERY`.

## User Review Required

> [!IMPORTANT]
> **3-Way Bridging UX**
> When a "HIGH" severity incident is reported, both the Fulfiller and Customer will see a new "Support Bridge" chat room appear. This room will include a Pikop Support Admin to facilitate real-time resolution.

## Verification Plan

### Manual Verification
1.  **Incident Reporting**: As a Fulfiller, report a "Safety Risk" incident. Verify that a "HIGH" severity dispute is created on the backend.
2.  **3-Way Bridge**: Confirm that a new conversation is created with both Fulfiller and Customer as participants.
3.  **Waiver Logic**: As an Admin, apply a "Cancellation Waiver" to a mission. Verify that the 25% penalty is reversed in the user's wallet ledger.
