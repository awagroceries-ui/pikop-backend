# Walkthrough - Strict Policy Enforcement & Legal Sync

I have successfully updated the Pikop platform's financial policies and synchronized the legal terms between the backend and Android app.

## Policy Adjustments

### 1. 25% Cancellation Fee (Pre-Pickup)
- **The Problem:** Previously, users could cancel missions for free even after an agent had been matched and was in transit to the pickup point.
- **The Fix:** Updated `cancelOrder` in the backend. If an agent is already matched, the system now automatically deducts a **25% Cancellation Penalty** from the user's wallet.
- **Communication:** The Android app now displays a specific warning if an agent is matched: *"An agent is already matched. Cancelling now will incur a 25% penalty fee. Proceed?"*

### 2. No Cancellation After Pickup
- **Strict Enforcement:** Once a mission status moves to `PICKED_UP`, the "Cancel Delivery" button is now hidden in the app.
- **Backend Guard:** The server will reject any cancellation attempt for an order that has already been picked up.

### 3. 75% Return Charge
- **Increased Rate:** Updated the return mission logic. If a delivery fails (e.g., recipient absent), the sender can initiate a return mission at **75% of the original fare** (increased from 50%).

## Technical Improvements

### 1. Centralized Legal Configuration
- **Live Terms:** Created a new `GET /api/v1/legal/config` endpoint that serves the latest Terms & Conditions and Privacy Policy in HTML format.
- **App Sync:** The Android app now fetches these terms live from the server. Any policy updates made by Awa Foods on the backend will instantly reflect in the app without requiring a store update.
- **WebView Rendering:** The `TermsScreen.kt` now uses a built-in browser engine to render high-quality, branded legal text.

## Verification Results

### Backend Logic
- Verified `cancelOrder` penalty triggers only when `fulfiller_id` is present.
- Verified `initiateReturn` uses the new `0.75` multiplier.
- **Result:** `PASS`.

### Android Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

## Deployment Instructions (VPS)
Please pull these policy updates to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
