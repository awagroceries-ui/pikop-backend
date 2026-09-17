# Task: Fulfiller SOS/Emergency Button

- [ ] **Database & Schema**
    - [ ] Create migration `1726520000000_emergency_sos_system.js`
- [ ] **Backend Core - SOS Logic**
    - [ ] Implement `triggerSOS` in `orderController.js`
    - [ ] Update `adminController.js` (getEmergencyDashboard, resolveEmergency)
    - [ ] Add backend routes (`orderRoutes.js`, `adminRoutes.js`)
- [ ] **Admin Dashboard UI**
    - [ ] Update `layout.ejs` with sticky SOS banner
    - [ ] Create `emergency_resolution.ejs` view
- [ ] **Android UI - SOS & Profile**
    - [ ] Update `ApiService.kt` (Add `triggerSOS` endpoint & Emergency Contact fields)
    - [ ] Update `ProfileEditScreen.kt` (Add Emergency Contact fields)
    - [ ] Update `ActiveOrderScreen.kt` (Implement SOS FAB with 3s hold logic)
- [ ] **Verification**
    - [ ] Test SOS trigger and Admin alert
    - [ ] Verify Emergency Contact SMS
    - [ ] Build and Deploy
