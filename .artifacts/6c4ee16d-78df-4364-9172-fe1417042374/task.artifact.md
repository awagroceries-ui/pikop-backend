# Task: Admin KYC Approval & Fulfiller Active Mission Persistence

- [x] **1. Admin Verification Queue KYC Approval Schema Fix (`adminController.js` & Migration)**
    - [x] Create migration `1726900000000_ensure_approved_at_column.js`
    - [x] Update `updateKYCStatus` in `adminController.js`
- [x] **2. Fulfiller Active Mission Persistence & Resume (`FulfillerDashboardScreen.kt` & `FulfillerOrdersScreen.kt`)**
    - [x] Update active mission check in `FulfillerDashboardScreen.kt` to cover all active non-terminal statuses
    - [x] Update `canResume` logic in `FulfillerOrdersScreen.kt`
- [x] **Verification & Deployment**
    - [x] Verify JS syntax using `node -c`
    - [x] Build release App Bundle (`app-release.aab`)
    - [x] Install release APK on connected Samsung Galaxy device
    - [x] Commit changes to local Git repository
