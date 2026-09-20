# Task: Final Playstore Hardening

- [ ] **Backend Security & Redaction**
    - [ ] Redact sensitive info in `authController.js` (Signup/Login logs)
    - [ ] Redact sensitive info in `adminController.js` (Login hash logs)
    - [ ] Redact PII in `fulfillerController.js` (KYC logs)
- [ ] **Backend Operational Stability**
    - [ ] Ensure atomicity in `updateKYCStatus` (Admin)
    - [ ] Harden `scheduledOrderJob.js` with try-catch and batch safety
- [ ] **Android UI Refinement**
    - [ ] Add "Tap to Copy" for Order ID in `TrackOrderScreen.kt`
    - [ ] Add "Tap to Copy" for Order ID in `ActiveOrderScreen.kt`
    - [ ] Implement "Clear Cart" warning in Storefront screens
    - [ ] Resolve unused code warnings across main screens
- [ ] **Final Verification**
    - [ ] Build and Deploy
