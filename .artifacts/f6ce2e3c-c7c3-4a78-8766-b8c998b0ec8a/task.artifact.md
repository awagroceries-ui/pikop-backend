# Task List - Merchant Growth: In-App Bulk Dispatch

- [ ] **Phase 1: Backend Session Bulk API**
    - [ ] Implement `createBulkOrdersSession` in `merchantController.js`
    - [ ] Wire wallet debiting logic for batch payments
    - [ ] Register new route in `merchantRoutes.js`
- [ ] **Phase 2: Android Bulk Interface**
    - [ ] Add bulk order models to `ApiService.kt`
    - [ ] Create `BulkDispatchScreen.kt` with dynamic row adding
    - [ ] Integrate address selection per row
- [ ] **Phase 3: Real-Time Batch Monitoring**
    - [ ] Update `MerchantPortalScreen.kt` with "Create Batch" button
    - [ ] Implement progress bar tracking for order batches
- [ ] **Verification**
    - [ ] Test multi-mission creation and automated wallet debiting
    - [ ] Git automation (Commit and Push)
