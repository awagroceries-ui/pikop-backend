# Implementation Plan - Fix Live Tracking & Marker Animation

This plan fixes the "Static Map" issue by establishing a reliable socket connection on the customer side and adding smooth marker animation for the fulfiller icon.

## Problem Description
1.  **Failing Socket Connection:** The customer app calls `SocketManager.connect()` without a user ID, causing the connection to abort. This prevents real-time location updates from reaching the map.
2.  **Jumping Marker:** When location updates do arrive, the marker jumps instantly to the new coordinate, providing a poor tracking experience.
3.  **Static ETA:** The ETA is only calculated occasionally and doesn't feel "live."

## Proposed Changes

### Android App

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- **Socket Connection:** Collect the `userId` from `TokenManager` and pass it to `SocketManager.connect(uid)` in the `DisposableEffect`.
- **Marker Animation:**
    - Introduce an `Animatable` (from Compose Animation) to track the "interpolated" location.
    - Every time a new `location_updated` event is received, launch a coroutine to animate the `interpolatedLatLng` from the old position to the new position over 1-2 seconds.
    - Bind the Fulfiller `Marker` to this animated state instead of the raw `fulfillerLocation`.
- **Dynamic ETA:**
    - Ensure `etaMinutes` is recalculated on every interpolated frame or at least every raw location update.
    - Add a "Live" badge or indicator near the ETA to show it's active.

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- **Tracking Reliability:**
    - Improve the location loop by checking for `socket?.connected()` before emitting.
    - Ensure the loop terminates strictly when the mission is no longer in an active status.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Socket Handshake:** Open the "Tracking" screen as a customer and verify the "Socket CONNECTED" log appears in Logcat.
2.  **Live Movement:** Using two devices (or a location simulator), move the fulfiller's position. Verify the blue "Agent" icon moves **smoothly** (sliding) rather than teleporting.
3.  **ETA Update:** Confirm that as the agent moves closer to the delivery point, the "Arriving in X mins" text decreases accurately.
4.  **Clean Disconnect:** Mark the mission as delivered and verify that the location loop in the fulfiller app stops (check for "update_mission_location" emissions in Logcat).
