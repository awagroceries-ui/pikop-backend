# Task: Business/Corporate Accounts (V3 Port)

- [/] **Database & Infrastructure**
    - [ ] Create migration `1726540000000_corporate_infrastructure.js`
- [ ] **Backend Core - Corporate Management**
    - [ ] Create `corporateController.js` in `backend_v3`
    - [ ] Add `corporateRoutes.js`
    - [ ] Update `app.js` to register routes
- [ ] **Backend - Billing Logic**
    - [ ] Implement `processCorporateDebit` in `walletService.js`
    - [ ] Update `orderController.js` to handle corporate Payer selection
- [ ] **Android UI Refinement**
    - [ ] Update `ApiService.kt` to match V3 corporate endpoints
    - [ ] Finalize `CorporateDashboardScreen.kt` (Limit management & spend reporting)
    - [ ] Update `OrderQuoteScreen.kt` for reliable billing method switching
- [ ] **Verification**
    - [ ] Test Corporate onboarding and approval
    - [ ] Verify staff invitation and limit enforcement
    - [ ] Verify spend reporting on dashboard
    - [ ] Build and Deploy
