# Task: Implement 100% Universal Tester Coupon (`TESTER100`)

- [ ] **Database Migration (Seed)**
    - [ ] Create `1726870000000_seed_universal_tester_coupon.js` for `TESTER100`
- [ ] **Backend Pricing & Activation Engines**
    - [ ] Update `orderController.js` for 100% discount on full order fare
    - [ ] Update `commerceController.js` for 100% discount & zero-cost checkout bypass
    - [ ] Update `paymentController.js` for zero-amount payment initialization guard
- [ ] **Android App Updates**
    - [ ] Update `OrderQuoteScreen.kt` to discount full fare for 100% percentage promos
- [ ] **Terminal Build & Verification**
    - [ ] Verify JS syntax using `node -c`
    - [ ] Build release bundle via terminal (`./gradlew :app:bundleRelease`)
    - [ ] Commit and push changes to GitHub
