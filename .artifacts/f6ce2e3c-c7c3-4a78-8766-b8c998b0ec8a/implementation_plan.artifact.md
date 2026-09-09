# Implementation Plan - Admin-Configurable COD Platform Fee

This plan converts the 10% COD platform fee from a hardcoded constant to a dynamic setting managed via the Admin Dashboard.

## User Review Required

> [!IMPORTANT]
> **Fee Freezing:** As requested, the platform fee amount is calculated and stored at the moment of order creation. Changing the rate in settings will only affect new missions; existing missions will retain the fee they were originally quoted.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725593000000_make_cod_fee_configurable.js)
- Initializes the `cod_fee_rate` key in the `settings` table with a default value of `0.10` (10%).

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getQuote`**:
    - Fetches the active `cod_fee_rate` from the `settings` table.
    - Uses the dynamic rate for the `platform_fee_amount` calculation.
    - **Bug Fix:** Fixed the logic order where `payer_type` was being used to calculate the SMS charge before it was actually determined.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- **`updateSettings`**:
    - Added support for updating `cod_fee_rate`.
    - Added validation to ensure the rate is between `0.00` and `0.50` (50% max).
    - Existing audit logging will capture these changes automatically.

#### [MODIFY] [settings.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/settings.ejs)
- Added an input field for "COD PLATFORM FEE" under the Pricing Dynamics section.
- Displayed as a percentage for better admin readability (e.g., input `10` for 10% and convert to `0.10` in the backend).

---

### Android App
- The app already uses the `platform_fee_amount` returned by the backend's quote API, so no changes are required on the mobile side. This ensures the app is always in sync with the server-side configuration.

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c src/controllers/orderController.js src/controllers/adminController.js`.

### Manual Verification
1.  **Rate Change:** Log in as admin, change the COD fee to 12% (0.12), and save.
2.  **Quote Check:** Request a delivery in the app for a ₦1,000 item. Verify the platform fee is now ₦120.
3.  **Persistence Check:** Change the rate back to 10% in admin. Verify the previously created mission still shows ₦120 in the admin tracking view.
4.  **Audit Log:** Verify the `audit_logs` table reflects the setting change.
