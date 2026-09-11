# Implementation Plan - Refine Live Map Animated Motion Tracking

This plan confirms the current tracking status and adds a final refinement to ensure "animated motion tracking" is perfectly smooth on both mobile and web surfaces.

## Current Status Confirmation

### Android App (User)
- **Status:** ✅ Fully Implemented and Functional.
- **Details:** Uses Compose `Animatable` for smooth interpolation. Markers are mobility-aware (walking, bike, car).

### Admin Dashboard & Guest Tracking
- **Status:** ⚠️ Partially Functional (Real-time but jumpy).
- **Details:** Position updates instantly via `setLatLng`, causing the marker to "teleport" instead of glide.

## Proposed Refinements

### Web Surfaces (Admin & Guest)

#### [MODIFY] [admin_track.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/admin_track.ejs)
#### [MODIFY] [guest_tracking.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_tracking.ejs)
- **Smooth Glide Animation:** Add a global CSS style to the Leaflet marker icons.
    ```css
    .leaflet-marker-icon {
        transition: transform 5s linear; /* Matches the typical location ping interval */
    }
    ```
- **Rationale:** This CSS transition allows the browser to handle the interpolation between the old and new `lat/lng` positions. When the fulfiller sends a location update, the marker will glide smoothly across the map instead of jumping.

---

## Verification Plan

### Manual Verification
1. **Android Tracking:** Confirm the agent marker moves smoothly as the agent travels.
2. **Admin Map:** Open a mission in "In Transit" status. Confirm the marker glides across the map during location updates.
3. **Guest Tracking:** Confirm the public tracking link provides the same smooth gliding experience for the recipient.
4. **Marker Accuracy:** Verify the car icon shows for car drivers and the bike icon shows for riders on all three screens.
