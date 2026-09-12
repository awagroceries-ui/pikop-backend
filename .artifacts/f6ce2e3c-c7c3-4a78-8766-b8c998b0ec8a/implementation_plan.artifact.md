# Implementation Plan - Dispatch Module Hardening Sprint

This plan addresses the final 15% of the Dispatch Module by implementing critical field operations for agents, timeout recovery for senders, and fixing the broken incident reporting pipeline.

## User Review Required

> [!IMPORTANT]
> **Fulfiller Failure Protocol:** Agents will now be able to mark a mission as "Failed - Recipient Absent" directly from the app.
> - **Requirement:** This button only becomes active after the agent has been at the destination for at least **10 minutes** (verified by the "Arrived" timestamp).
> - **Evidence:** Agents must capture a photo of the delivery location as proof of their attempt before the "Fail" action is processed.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`fileIncident` [NEW]**: Implement the logic to record agent-reported incidents (breakdowns, safety risks) into the `disputes` or a new `incidents` table.
- **`dispatchService.js`**: Update `findNearbyFulfillers` to accept a radius parameter and implement an automated 3-step expansion (20km -> 40km -> 60km) if no agents are found initially.

#### [MODIFY] [orderRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/orderRoutes.js)
- Register `POST /api/v1/orders/:id/incident` to fix the current 404 error in the Fulfiller app.

---

### Android App

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- **Logistics Tools:**
    - Add a **"Request Leave-at-Door Consent"** button. This triggers the SMS/Push link to the receiver.
    - Add a **"Mark Failed (Recipient Absent)"** button.
    - **Timer Logic:** Implement a 10-minute countdown that starts when the agent clicks "Confirm Arrival." The "Mark Failed" button remains disabled until this timer hits zero.
    - **Evidence Capture:** Integrate the camera for the "Failed" flow to ensure the agent provides proof of the attempt.

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- **Timeout Resolution:**
    - Detect if the mission is in `ACKNOWLEDGMENT_TIMEOUT` state (sent via deep-link or status update).
    - Show a high-priority banner or dialog with two choices:
        1. **"Proceed Anyway":** Force dispatch using the original address.
        2. **"Abort Mission":** Cancel the request (standard cancellation policy applies).

---

## Verification Plan

### Manual Verification
1.  **Incident Test:** Report a "Breakdown" as an agent. Verify the server returns 200 and the incident appears in the Admin Panel.
2.  **Consent Test:** Arrive at destination. Request consent. Verify the receiver gets the link and the mission updates to "Delivered" once they approve.
3.  **Timeout Choice:** Simulate a 2-hour receiver silence. As the sender, click "Proceed Anyway" and verify the mission moves to the fulfiller search queue.
4.  **Radius Expansion:** Create a mission in a remote area. Observe the logs to see the dispatch engine expanding from 20km to 60km.
