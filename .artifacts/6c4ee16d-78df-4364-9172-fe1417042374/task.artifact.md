# Task: Audit-Driven Fixes & Hardening

- [ ] **Backend - Financial Hardening**
    - [ ] Update `processMissionSettlement` with idempotency check
    - [ ] Update `releaseEscrow` with idempotency check
    - [ ] Update `processReferralReward` with idempotency check
- [ ] **Backend - State Machine & Roles**
    - [ ] Implement `rescheduleOrder` in `orderController.js`
    - [ ] Update `updateMerchantKYCStatus` in `adminController.js` to preserve roles
- [ ] **Android UI Polishing**
    - [ ] Add article counts to `SupportHubScreen.kt`
    - [ ] Improve scheduling validation feedback in `OrderQuoteScreen.kt`
- [ ] **Verification**
    - [ ] Test duplicate settlement prevention
    - [ ] Verify role preservation
    - [ ] Build and Deploy
