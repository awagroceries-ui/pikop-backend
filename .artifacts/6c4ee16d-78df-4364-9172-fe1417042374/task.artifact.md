# Task: Admin KYC Approval & Fulfiller Active Mission Persistence

- [ ] **1. Admin Verification Queue KYC Approval Schema Fix (`adminController.js` & Migration)**
    - [ ] Create migration `1726900000000_ensure_approved_at_column.js`
    - [ ] Update `updateKYCStatus` in `adminController.js`
- [ ] **2. Fulfiller Active Mission Persistence & Resume (`FulfillerDashboardScreen.kt` & `FulfillerOrdersScreen.kt`)**
    - [ ] Update active mission check in `FulfillerDashboardScreen.kt` to cover all active non-terminal statuses
    - [ ] Update `canResume` logic in `FulfillerOrdersScreen.kt`
- [ ] **Verification & Deployment**
    - [ ] Verify JS syntax using `node -c`
    - [ ] Build release App Bundle (`app-release.aab`)
    - [ ] Install release APK on connected Samsung Galaxy device
    - [ ] Commit and push all changes to GitHub
