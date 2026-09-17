# Implementation Plan - Fulfiller SOS & Emergency Response

This plan implements a high-priority SOS system for Fulfillers to signal for help during active missions, alerting both Pikop Admins and optional trusted contacts.

## Proposed Changes

### 1. Database & Schema

#### [NEW] [emergency_sos_system migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726520000000_emergency_sos_system.js)
- **Extend `fulfillers` table**: Add `emergency_contact_name` and `emergency_contact_phone`.
- **[NEW] `emergency_alerts` table**:
    - `id` (SERIAL PRIMARY KEY)
    - `order_id` (INT, references orders)
    - `fulfiller_id` (INT, references fulfillers)
    - `last_location` (GEOGRAPHY)
    - `status` (OPEN, RESOLVED)
    - `resolution_notes` (TEXT)
    - `created_at` (TIMESTAMP)

### 2. Backend Logic (Node.js)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Implement `triggerSOS`**:
    - Record the alert in `emergency_alerts`.
    - Broadcast a high-priority `emergency_alert` event to all `admins` via Socket.io.
    - If the fulfiller has an emergency contact, trigger a specialized Termii SMS with the Fulfiller's live tracking link.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- `getEmergencyDashboard`: List all active/open SOS alerts.
- `resolveEmergency`: Mark an alert as resolved and log the intervention taken (e.g., "Contacted authorities").

### 3. Admin Dashboard (V3 Command Core)

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/layout.ejs)
- Add a **Sticky Emergency Banner** at the top of the dashboard that appears whenever an SOS is `OPEN`.
- The banner will link directly to the emergency resolution view.

#### [NEW] `emergency_resolution.ejs`
- Detail view showing the Fulfiller's profile, live location on a map, mission context, and contact buttons.

### 4. Android Frontend (Compose)

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- **Add SOS FAB**: A prominent red FAB labeled "SOS".
- **Trigger Mechanism**: Requires a **3-second hold** to activate (using a circular progress indicator to show intent) to prevent accidental triggers.
- **Continuous Tracking**: Once SOS is active, the app will increase location ping frequency to every 5 seconds.

#### [MODIFY] [ProfileEditScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/ProfileEditScreen.kt)
- Add optional fields for **Emergency Contact Name** and **Phone Number**.

## User Review Required

> [!IMPORTANT]
> **Accidental Trigger Prevention**
> I am implementing a "Hold to Trigger" pattern (3 seconds). This is faster than a dialog confirmation but significantly more intentional than a single tap.

> [!NOTE]
> **SMS Charges**
> SOS-triggered SMS to trusted contacts will be charged the standard ₦50 fee to the platform (logged as `PURPOSE: EMERGENCY_ALERT`).

## Verification Plan

### Manual Verification
1.  **Trigger SOS**: During an active mission, hold the SOS button for 3 seconds.
    - Verify the "Emergency" state appears in the app.
    - Verify the Admin Dashboard immediately displays the high-visibility red banner.
2.  **Location Stream**: Move the device while SOS is active. Verify the Admin map updates every 5 seconds.
3.  **Emergency Contact**: Set a test emergency contact. Trigger SOS and verify an SMS is delivered with the location link.
4.  **Resolution**: Resolve the SOS via Admin. Verify the red banner disappears.
