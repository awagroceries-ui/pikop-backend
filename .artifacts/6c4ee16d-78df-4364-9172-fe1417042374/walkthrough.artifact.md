# Walkthrough - Advanced Incident & Dispute Engine

I have successfully implemented the Advanced Incident & Dispute Engine, providing the platform with professional tools to manage fleet friction, marketplace disputes, and automated financial waivers.

## Changes Made

### 🛠️ Infrastructure & Schema
- **Migration**: Created `1726490000000_incident_management.js` which:
    - Extended the `disputes` table with `severity` (LOW, MEDIUM, HIGH), `incident_category`, and `is_3way_bridged`.
    - Created the `conversation_participants` table to support multi-party chat rooms.
    - Updated the wallet ledger to support `PENALTY_WAIVER` and `RETURN_WAIVER` purposes.

### 🧠 Backend Logic (Intelligent Resolution)
- **Severity-Based Responses**: Updated `orderController.js` so that reporting a "HIGH" severity incident (e.g., Accident or Safety Risk) automatically activates a **3-Way Support Bridge**.
- **Support Bridge**: Implemented a new helper that joins the Customer, Fulfiller, and an assigned Support Admin into a single real-time conversation for immediate mediation.
- **Automated Waivers**: Added `applyAutomatedWaiver` to `walletService.js`. This allows admins to instantly reverse the **25% cancellation penalty** or **75% return fee** with a single click, crediting the user's wallet automatically.

### 📊 Admin Resolution Dashboard
- **Dispute Resolution Center**: Created a new, dedicated view in the Admin Dashboard (`/admin/disputes`) prioritized by severity.
- **One-Click Resolution**: Admins can now choose between:
    - **Release Escrow**: standard marketplace release.
    - **Refund Buyer**: standard marketplace refund.
    - **Apply Waiver**: reverses specific penalties based on incident validity.

### 📱 Android UI (Structured Reporting)
- **Fulfiller Incidents**: Updated the `ActiveOrderScreen.kt` reporting dialog to include severity levels and clearer categories like `VEHICLE_BREAKDOWN` and `ACCIDENT`.
- **Customer Disputes**: Redesigned the `SecurePayDisputeDialog` in `TrackOrderScreen.kt` with a more structured layout, allowing customers to specify severity and precise issues like `INCORRECT_ITEM` or `ITEM_DAMAGED`.

## Verification Results
- **Android Build**: Successfully compiled (`:app:assembleDebug`).
- **Logic Integrity**:
    - Confirmed "HIGH" severity reports trigger the 3-way bridge creation logic.
    - Verified that waivers correctly calculate the 25% or 75% amounts and record them as `CREDIT` in the ledger.
- **Visual Audit**: Admin dashboard correctly highlights "HIGH" priority incidents in red for immediate attention.

## Deployment Instructions
To activate the new incident engine on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
