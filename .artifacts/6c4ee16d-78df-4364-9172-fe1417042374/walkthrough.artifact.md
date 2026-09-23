# Walkthrough - Fulfiller Conduct Policy Route & Order Acceptance Fix

I have resolved the "Cannot GET /terms/fulfiller" error and fixed the Fulfiller order acceptance response mapping bug.

## Changes Made

### 📜 1. Fulfiller Conduct Policy Routes (`legalRoutes.js`, `legalController.js`, `app.js`, `MainActivity.kt`)
- **Backend Routes**: Added `/terms/fulfiller` and `/terms-fulfiller` routes to `legalRoutes.js` and mounted `app.use('/terms', require('./routes/legalRoutes'))` in `app.js`.
- **Legal Controller**: Implemented `getFulfillerTerms` in `legalController.js` to render the Fulfiller Terms & Conduct policy without `404 Cannot GET` errors.
- **Android Viewer**: Updated `terms_viewer/{showFulfillerTerms}` in `MainActivity.kt` to load `https://api.pikop.com.ng/legal/terms/fulfiller` when Fulfiller terms are requested.

---

### 🚴 2. Fulfiller Order Acceptance & Navigation Fix (`orderController.js`, `ApiService.kt`, `FulfillerDashboardScreen.kt`)
- **Root Cause Identified**: The backend `acceptOrder` returned `{ success: true, status: 'MATCHED', data: { status: 'MATCHED' } }`. However, the app's `OrderResponse` data class only looked for top-level `status` which was missing from the backend response JSON before this fix, causing `response.status` to evaluate to `null`. The app assumed claim failure and called `fetchDashboardData()`, causing the accepted offer to disappear from available offers without navigating to active tracking.
- **Backend Fix (`orderController.js`)**: Updated `acceptOrder` response to include `status: resStatus` at the top-level of the JSON object.
- **Android Response Model (`ApiService.kt`)**: Added `data: OrderResponseData? = null` to `OrderResponse` so `response.data?.status` is parsed.
- **Accept Handler (`FulfillerDashboardScreen.kt`)**: Updated `onAccept` to check `response.status ?: response.data?.status`. On `MATCHED` or `QUEUED`, it displays *"Mission Accepted!"* and navigates immediately to `active_order/$orderId`.
- **Active Mission Resume Banner**: Added a top-level **"ACTIVE MISSION IN PROGRESS 🚀"** banner at the top of the Fulfiller Dashboard so Fulfillers can tap **"RESUME"** to return to active mission navigation at any time.

---

## Verification Results

- **Syntax Check**: [VERIFIED] All modified Node.js files passed syntax checks (`node -c`).
- **APK Installed**: [SUCCESS] Freshly installed and launched on connected Samsung Galaxy test device (`SM-S918W`).
- **App Bundle**: [SUCCESS] Rebuilt Play Store App Bundle (`app-release.aab`).
- **Local Commit**: [SUCCESS] Created local commit `aaff380c`.

---

## Deployment Instructions

1. **Push Local Commit**:
   Run `git push` in your local terminal to publish the commits to GitHub.

2. **Update VPS Server**:
   Run these commands on your VPS terminal (`root@srv1932412`):
   ```bash
   cd /var/www/pikop-api/backend_v3/backend_v3
   git pull origin main
   pm2 restart pikop-v3
   ```
