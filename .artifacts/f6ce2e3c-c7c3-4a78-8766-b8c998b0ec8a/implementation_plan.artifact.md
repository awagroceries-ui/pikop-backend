# Implementation Plan - Fix Admin Real-Time Mission Tracking

This plan fixes the issue where the admin dashboard tracking screen fails to update mission status and agent details in real-time, appearing "stuck."

## Diagnostic Findings
1.  **Event Name Mismatch:** The backend (`socketService.js`) emits `location_updated`, but the admin tracking page (`admin_track.ejs`) was listening for `location_changed`. This broke live GPS tracking for admins.
2.  **Selective Status Updates:** The tracking page only reloaded on `DELIVERED` or `CANCELLED` events. It ignored intermediate status changes like `MATCHED` (Agent accepted) or `PICKED_UP`, which are critical for displaying who the active agent is.
3.  **Static Agent Section:** The agent details (Name, ID, Phone) are only rendered on the initial server-side page load. Without a reload or a complex real-time UI update, this section remains empty even after an agent accepts the mission.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [admin_track.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/admin_track.ejs)
- **Unified Location Listener:** Change the socket listener from `location_changed` to `location_updated` to match the V3 backend and other tracking surfaces.
- **Universal Status Sync:** Update the `status_updated` listener to trigger a `window.location.reload()` for **any** status change.
    - **Rationale:** This ensures the entire sidebar—including the "Active Agent" details, "Order Ledger," and "Status Badge"—is updated with the latest authoritative data from the database immediately upon any mission event.
- **UI Refinement:** Ensure the "Active Agent" header and agent ID are prominent and follow the requested format: "Agent Name (#ID)".

---

## Verification Plan

### Manual Verification
1.  **Acceptance Sync:** Open the admin tracking screen for a "SEARCHING" mission. Accept the mission as a fulfiller. Verify the admin screen reloads automatically and displays the agent's name and ID.
2.  **Live GPS:** Move the fulfiller's location (via emulator or real device). Verify the agent marker moves on the admin map.
3.  **Full Lifecycle:** Progress the mission through "Picked Up" and "Arrived." Verify the status badge updates on the admin screen at each stage.
4.  **Final Closure:** Complete the mission. Verify the admin screen reloads to the final "DELIVERED" state.
