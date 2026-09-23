# 📌 Task Checklist: Admin Corporate Verification & Fulfiller Active/Queued Missions Fix

- `[x]` Task 1: Database Migration & Backend Corporate Verification Fix
  - `[x]` Create database migration `1726910000000_add_cac_document_url_to_corporate_accounts.js`
  - `[x]` Update `corporateController.js` to handle `cac_document_url` and default status to `PENDING_VERIFICATION`
  - `[x]` Update `adminController.js` `updateCorporateStatus` and `getCorporateAdmin`
  - `[x]` Update `corporate_admin.ejs` status badges, modal options, and add "View Document" link

- `[x]` Task 2: Backend Fulfiller Active & Queued Missions Fix
  - `[x]` Update `getFulfillerOrders` in `orderController.js` to support robust ID matching and explicit filters
  - `[x]` Implement Queue Auto-Promotion in `orderController.js` when an active mission is completed

- `[x]` Task 3: Android App - Corporate Onboarding Enhancements
  - `[x]` Update `ApiService.kt` `CreateCorporateRequest` data class
  - `[x]` Update `CorporateBusinessSetupScreen.kt` to include document URL input and pending verification banner

- `[x]` Task 4: Android App - Fulfiller Missions & Dashboard Enhancements
  - `[x]` Update `FulfillerOrdersScreen.kt` with Tab Navigation (`Active`, `Queued`, `Completed`) and RESUME/START buttons
  - `[x]` Update `FulfillerDashboardScreen.kt` with Active and Queued mission banners

- `[x]` Task 5: Verification & Testing
  - `[x]` Run database migrations
  - `[x]` Verify Admin Corporate Dashboard
  - `[x]` Verify Fulfiller Mission Records and Active/Queued navigation
