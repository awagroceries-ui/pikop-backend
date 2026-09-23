# 📋 Implementation Plan: Admin Corporate Verification & Fulfiller Active/Queued Missions Fix

Address two critical operational issues across the Pikop platform:
1. Fix Admin Dashboard corporate verification approval HTTP 500 error & enable CAC / Business verification document upload, display, and review.
2. Fix Fulfiller Dashboard & Mission Records so agents can view, resume, and start accepted, active, and queued missions seamlessly.

---

## 🔍 Root Cause Analysis

### Issue 1: Admin Dashboard Corporate Verification Error & Missing Documents
- **SQL Crash on Approval**: `adminController.js` (`updateCorporateStatus`) attempted to run an SQL `UPDATE` on non-existent columns `is_active` and `monthly_credit_limit` on `corporate_accounts`. The table schema uses `status` (`'PENDING_VERIFICATION'`, `'ACTIVE'`, `'SUSPENDED'`, `'REJECTED'`).
- **Missing Document Upload**: `setupCorporateProfile` (`corporateController.js`) and `CorporateBusinessSetupScreen.kt` only accepted metadata strings (`company_name`, `cac_number`, `business_address`, `billing_email`). No document file/URL was requested or saved, and profiles defaulted to `ACTIVE` automatically instead of `PENDING_VERIFICATION`.
- **Missing Admin Review View**: `corporate_admin.ejs` evaluated `acc.is_active` (which was `undefined`), always showing a red `SUSPENDED` badge, and lacked UI to inspect CAC / verification documents.

### Issue 2: Fulfiller Active & Queued Missions Visibility & Resume
- **Lack of Queue Auto-Promotion**: When a fulfiller completed an active mission (`verifyDelivery` / `updateStatus`), backend did not automatically promote queued missions (`queued_for_fulfiller_id = fId AND status = 'QUEUED'`) to `MATCHED` / active status.
- **Backend Query Scope**: `getFulfillerOrders` in `orderController.js` lacked robust ID matching (didn't check both `fulfillers.id` and `users.id`) and lacked explicit filter support for `'queued'`.
- **Mission Records Screen Limitations**: `FulfillerOrdersScreen.kt` displayed a flat list under "Delivery History" without Tab navigation for **Active**, **Queued**, and **Completed** missions, making it hard for agents to filter and manage ongoing or queued jobs.
- **Dashboard Banner Coverage**: `FulfillerDashboardScreen.kt` needed robust active/queued mission banners with direct **RESUME** / **START** action buttons to ensure agents can immediately continue work.

---

## 🛠️ Proposed Changes

### Component 1: Database Migration & Backend API Enhancements

#### [NEW] [1726910000000_add_cac_document_url_to_corporate_accounts.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726910000000_add_cac_document_url_to_corporate_accounts.js)
- Add `cac_document_url` (`text`) column to `corporate_accounts`.
- Ensure default status is `'PENDING_VERIFICATION'`.

#### [MODIFY] [corporateController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/corporateController.js)
- Update `setupCorporateProfile`:
  - Accept `cac_document_url` in request body.
  - Set default status to `'PENDING_VERIFICATION'` for new corporate accounts.
  - Log/store document in `kyc_documents` (`doc_type = 'CAC_CERTIFICATE'`).

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update `getCorporateAdmin`:
  - Fetch `cac_document_url` and associated `kyc_documents`.
- Update `updateCorporateStatus`:
  - Execute `UPDATE corporate_accounts SET status = $1 WHERE id = $2` with valid statuses (`ACTIVE`, `PENDING_VERIFICATION`, `SUSPENDED`, `REJECTED`).

#### [MODIFY] [corporate_admin.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/corporate_admin.ejs)
- Update status badge rendering: `ACTIVE` (Green), `PENDING_VERIFICATION` (Yellow), `SUSPENDED` / `REJECTED` (Red).
- Add **"View Document"** button/link to view CAC certificate / verification proof.
- Update modal select options to allow setting status to `ACTIVE`, `PENDING_VERIFICATION`, `SUSPENDED`, or `REJECTED`.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update `getFulfillerOrders`:
  - Check both `fulfillers.id` and `users.id` for `fulfiller_id` and `queued_for_fulfiller_id`.
  - Add explicit filter handling for `active`, `queued`, `completed`, and `all`.
- Add **Queue Auto-Promotion** in delivery completion (`verifyDelivery` & `updateStatus`):
  - When an active mission transitions to `DELIVERED` or `RELEASED`, query for the oldest queued mission (`queued_for_fulfiller_id = fulfillerId AND status = 'QUEUED'`).
  - Automatically promote queued mission: set `fulfiller_id = fulfillerId`, `status = 'MATCHED'`, `matched_at = CURRENT_TIMESTAMP`.
  - Emit socket event (`status_updated`) to notify the fulfiller immediately.

---

### Component 2: Android Mobile App (`:app`)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `CreateCorporateRequest` model to include `cac_document_url: String? = null`.
- Ensure `getFulfillerOrders()` query parameters or response model handle active, queued, and completed missions.

#### [MODIFY] [CorporateBusinessSetupScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/CorporateBusinessSetupScreen.kt)
- Add CAC Document / Certificate URL input field during business onboarding.
- Display status notice indicating account submission for verification under Admin review.

#### [MODIFY] [FulfillerOrdersScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerOrdersScreen.kt)
- Implement **Tab Navigation**: `Active Missions`, `Queued Missions`, and `Completed History`.
- Render **RESUME MISSION** button for Active missions and **START QUEUED MISSION** button for Queued missions, navigating directly to the active order tracking screen (`onNavigateToActiveOrder(orderId)`).

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Add prominent **Active Mission Banner** ("ACTIVE MISSION IN PROGRESS") and **Queued Mission Banner** ("QUEUED MISSION READY") at the top of the dashboard.
- Provide instant **RESUME** / **START** buttons to jump into active navigation.

---

## 🧪 Verification Plan

### Backend & Migration Verification
1. Execute migration: `npm run migrate:up`.
2. Test corporate signup: verify profile created with `status = 'PENDING_VERIFICATION'` and `cac_document_url`.
3. Test Admin Corporate Dashboard (`/admin/corporate`): verify status badge, CAC document preview, and error-free status updates to `ACTIVE`.
4. Test Queue Auto-Promotion: complete an active mission and verify queued mission automatically promotes to `MATCHED`.

### Android Mobile UI Verification
1. **Corporate Setup**: Verify business registration flow accepts document URL and displays "Pending Verification".
2. **Fulfiller Dashboard & Mission Records**:
   - Verify Active and Queued mission banners appear on the dashboard.
   - Switch tabs in Mission Records ("Active", "Queued", "Completed") and verify missions are categorized correctly.
   - Tap "RESUME" / "START" and confirm smooth navigation to `active_order/$orderId`.
