# 🚀 Pikop Platform V3 - Comprehensive Project Handover Note

**Date**: September 23, 2026
**Project**: Pikop Logistics, Marketplace, Fleet & Corporate Billing Platform
**Current Mobile Version**: `v1.0.3` (Version Code `6`)
**Target Environment**: Android (Jetpack Compose / Material 3) & Node.js/Express PostgreSQL Backend (VPS)

---

## 1. 📌 Executive Summary

Pikop is a multi-platform logistics, food/grocery delivery, marketplace, and corporate billing ecosystem tailored for the Nigerian market. The codebase consists of:
1. **Android App (`:app`)**: Kotlin, Jetpack Compose, Material 3, Google Maps Compose, Retrofit, Coroutines/Flow, DataStore, Socket.io Client.
2. **Backend API (`backend_v3`)**: Node.js, Express, PostgreSQL with PostGIS extension, EJS Admin Dashboard, Socket.io, Paystack, Termii SMS, Firebase Cloud Messaging (FCM), Google Generative AI (Gemini v3).

All recent critical bug fixes, UI/UX refinements, database migrations, and API enhancements have been compiled, verified, and pushed to `origin/main` on GitHub (`https://github.com/awagroceries-ui/pikop-backend.git`).

---

## 2. 📱 Android App Status & Build Artifacts

- **App Package**: `com.ng.pikop`
- **Main Activity**: `com.ng.pikop.MainActivity`
- **Production App Bundle (.aab)**:
  `app/build/outputs/bundle/release/app-release.aab`
- **Signed Release APK (.apk)**:
  `app/build/outputs/apk/release/app-release.apk`
- **Connected Test Device**: Samsung Galaxy S23 Ultra (`SM-S918W`) over Wireless ADB (`adb-R5CW407V6AA-e2q70z._adb-tls-connect._tcp`).

---

## 3. 🛠️ Key Architectural Modules & Features

### 🛵 1. Customer Module (Dispatch & Marketplace)
- **Services**: Dispatch (Small/Medium/Large), Food (Kitchens), Groceries (Vendors), Shop (Merchants).
- **Cart & Checkout**: Multi-item shopping cart, item quantity controls, COD Escrow, Paystack Card checkout, and Wallet Pay.
- **Universal Tester Coupon (`TESTER100`)**: Waives **100% of the entire order total** (Item Price + Delivery Fee + Escrow/Platform Fee + SMS Charges = **₦0.00**). Automatically bypasses payment gateways for zero-cost testing.
- **Real-Road Network Navigation**: Map polylines follow actual streets, turns, and highways via multi-segment curve pathing (`createRoadPolyline()`) rather than straight dashed lines.

### 🚴 2. Fulfiller / Fleet Module
- **Onboarding & Verification**: Onboarding UI (Material 3 DatePicker, Gender Selector, Nigeria State/City dropdowns), vehicle plate verification, Didit identity webhook integration.
- **Mission Claiming (`acceptOrder`)**: Idempotent mission acceptance handling. When a Fulfiller taps "ACCEPT MISSION", the API returns `{ success: true, status: 'MATCHED', data: { status: 'MATCHED' } }`, immediately transitioning the UI to `active_order/$orderId`.
- **Active Mission Resume Banner**: A prominent **"ACTIVE MISSION IN PROGRESS 🚀"** banner is rendered at the top of the Fulfiller Dashboard for any in-progress order (`status NOT IN ('DELIVERED', 'CANCELLED', 'RELEASED', 'REFUNDED')`). Fulfillers can tap **"RESUME"** to return to active mission navigation at any time.
- **Map Marker Scaling**: Fulfiller marker icons (`marker_bike.png`, `marker_car.png`, `marker_walking.png`) are scaled dynamically to a crisp **`38dp x 38dp`** size based on display density (`getScaledMarkerIcon()`).

### 🏪 3. Merchant & Kitchen Module
- **Merchant Onboarding & KYC**: Business registration with CAC and NAFDAC support. Stored safely in `kyc_documents` via `user_id`.
- **Unique Store Links**: Deep-link sharing support (`pikop://store/<slug>`).
- **Batching & Promos**: Merchant batch creation and store-specific promotional coupons.

### 🏢 4. Corporate Accounts Module
- **Team Billing**: Centralized corporate wallet, monthly credit limits, daily spend limits, and authorized staff sub-accounts.
- **Business Setup**: Business Verification onboarding in `corporateController.js` `setupCorporateProfile` with automatic `status = 'ACTIVE'` activation and wallet creation.

### 🛡️ 5. Admin Command Dashboard (`/admin`)
- **EJS Views**: Responsive dashboard themed in Pikop Brand Green (`#008751`), Gold (`#FFC618`), and White.
- **Verification Queue (`/admin/kyc`)**: Approve/Reject Fulfiller KYC documents. Sets `approved_at = CURRENT_TIMESTAMP` and sends custom welcome emails.
- **Forced User Account Deletion (`/admin/users/:id/force-delete`)**: Allows Admins to permanently purge unverified or malformed user accounts directly from `customer_detail.ejs` and `fulfiller_detail.ejs`.
- **AI Knowledge Base Manager (`/admin/knowledge-base`)**: Manage knowledge articles used by the **Pikop AI Agent** (`askPikopAgent`) and in-app FAQs.
- **Corporate Account Manager (`/admin/corporate`)**: View, approve, or adjust corporate credit limits.
- **Audit Logs Viewer (`/admin/audit-logs`)**: Review security audit logs and process Play Store / NDPA Web Account Deletion requests.
- **Marketplace Returns Dashboard (`/admin/returns`)**: Monitor customer return requests, merchant notes, and refund statuses.

---

## 4. 🔑 Recent Major Bug Fixes & Improvements Completed

| Issue | Root Cause | Fix Applied |
| :--- | :--- | :--- |
| **"Cannot GET /terms/fulfiller"** | Express router mounted at `/terms` stripped `/terms`, looking for sub-path `/terms/fulfiller` instead of `/fulfiller`. | Added `/fulfiller`, `/terms/fulfiller`, and `/terms-fulfiller` routes in `legalRoutes.js` and top-level fallbacks in `app.js`. Updated `MainActivity.kt` to load `https://api.pikop.com.ng/legal/terms/fulfiller`. |
| **Fulfiller Order Acceptance Loop** | `OrderResponse` model expected top-level `status` which was missing from `acceptOrder` JSON before fix. | Updated `acceptOrder` in `orderController.js` to return `status` at top-level and inside `data`. Updated `ApiService.kt` and `FulfillerDashboardScreen.kt` to check `response.status ?: response.data?.status`. |
| **Admin KYC Approval Crash** | `approved_at` column was missing from `fulfillers` table in live database. | Created migration `1726900000000_ensure_approved_at_column.js` (`ALTER TABLE "fulfillers" ADD COLUMN IF NOT EXISTS "approved_at" timestamp;`). |
| **Admin Force Deliver Transaction Abort** | SQL error in sub-operations (`processMissionSettlement` / `releaseEscrow`) marked PostgreSQL transaction as ABORTED. | Wrapped sub-operations in `SAVEPOINT` blocks (`settlement_sp` & `escrow_sp`) in `adminController.js` `updateOrderStatus`. |
| **Quote Calculation NaN Error** | JS passed `NaN` to PostgreSQL numeric column `total_fare: decimal(12,2)`. | Added `safeNumber(val, fallback)` sanitizer function in `orderController.js` for all numeric calculations. |
| **DNS Resolution Failure on Samsung Devices** | Samsung Netd blocked DNS for `.com.ng` (`UnknownHostException`). | Implemented resilient OkHttp `Dns` fallback in `ApiService.kt` pointing to `168.231.113.202`. |
| **Merchant Signup Verification Crash** | `merchantController.js` tried inserting into `kyc_documents (user_id, ...)` when `user_id` column was missing. | Created migration `1726890000000_add_user_id_to_kyc_documents.js` making `fulfiller_id` nullable and adding `user_id`. Updated `merchantController.js`. |
| **Business Account Setup Crash** | `setupCorporateProfile` used `ON CONFLICT (owner_user_id)` without a `UNIQUE` constraint. | Created migration `1726880000000_add_unique_constraint_to_corporate_accounts.js` and updated `corporateController.js` to use explicit SELECT/UPDATE/INSERT. |
| **Role Selector Icon Contrast & Sizing** | Near-black icon was invisible in Dark Mode; tall layout required scrolling. | Changed icon color to Dodger Blue (`#2196F3`), resized logo to `70dp`, card height to `100dp`, and icon size to `30dp` in `UserTypeSelectionScreen.kt`. |
| **Oversized Map Tracking Icon** | Raw drawable PNGs rendered 1:1 at full resolution on Google Maps. | Created `getScaledMarkerIcon()` scaling marker bitmaps to `38dp x 38dp` in `TrackOrderScreen.kt`. |

---

## 5. 🗄️ Database Migrations History

All database migrations are located in `backend_v3/migrations/`:
- `1723800000000_v3_core_schema.js`
- `1723810000000_v3_logistics_schema.js`
- `1723830000000_v3_business_entities.js`
- `1724080000000_create_kyc_documents.js`
- `1724100000000_add_approved_at_to_fulfillers.js`
- `1726500000000_fleet_partner_program.js`
- `1726540000000_corporate_infrastructure.js`
- `1726870000000_seed_universal_tester_coupon.js`
- `1726880000000_add_unique_constraint_to_corporate_accounts.js`
- `1726890000000_add_user_id_to_kyc_documents.js`
- `1726900000000_ensure_approved_at_column.js`

To run pending migrations on any environment:
```bash
npm run migrate:up
```

---

## 6. 🚀 Server Deployment Protocol (VPS)

**Server IP / Host**: `api.pikop.com.ng` (`168.231.113.202` / `root@srv1932412`)
**Application Directory**: `/var/www/pikop-api/backend_v3/backend_v3`
**Process Manager**: PM2 (`pikop-v3`)

### Deployment Commands:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

### Server Logs Inspection:
```bash
pm2 logs pikop-v3 --lines 100
```

---

## 7. 🔑 Testing & Testing Environment Credentials

- **Production API URL**: `https://api.pikop.com.ng`
- **Universal Tester Coupon**: **`TESTER100`** (100% discount on entire order fare)
- **Master OTP Code**: `123456`
- **Admin Dashboard**: `https://api.pikop.com.ng/admin/login`
- **Legal & Policy Terms**: `https://api.pikop.com.ng/legal/terms/fulfiller`

---

## 8. 🎯 Recommendations for Next Development Sessions

1. **Keep Mobile App & Backend Sync**: Always ensure newly built `.apk` / `.aab` artifacts correspond to the deployed PM2 backend version.
2. **Database Savepoints**: When adding complex multi-step database procedures inside `client.query('BEGIN')`, wrap optional sub-queries in `SAVEPOINT` blocks to avoid aborting the outer transaction.
3. **Web Account Deletion Compliance**: Periodically check `/admin/audit-logs` for `WEB_DELETE_ACCOUNT_REQUEST` events to comply with Google Play & NDPA 14-day deletion rules.
