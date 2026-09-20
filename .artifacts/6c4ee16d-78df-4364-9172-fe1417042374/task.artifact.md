# Task: System Hardening & Bug Fixes

- [ ] **Backend - Financial & State Hardening**
    - [ ] Update `paymentController.js`: Atomic duplicate creation checks
    - [ ] Update `orderController.js`: Fulfiller-level locking in `acceptOrder`
    - [ ] Update `scheduledOrderJob.js`: Batch processing with `LIMIT 50`
- [ ] **Backend - Security & Stability**
    - [ ] Update `marketplaceController.js`: Restrict field exposure in `getVendorDetails`
    - [ ] Update `kitchenController.js`: Restrict field exposure in `getKitchenDetails`
    - [ ] Update `commerceController.js`: Safe JSON parsing for operating hours
- [ ] **Android - UI Resilience**
    - [ ] Update `SupportHubScreen.kt`: Error state and Retry button
    - [ ] Update `ActiveOrderScreen.kt`: Button debouncing during API calls
- [ ] **Verification**
    - [ ] Verify atomic mission creation
    - [ ] Verify fulfillment race condition fix
    - [ ] Build and Deploy
