# Task List - Merchant Growth: In-App Bulk Dispatch

- [x] **Phase 1: Backend Session Bulk API**
    - [x] Implement `createBulkOrdersSession` in `merchantController.js`
    - [x] Wire wallet debiting logic for batch payments
    - [x] Register new route in `merchantRoutes.js`
- [x] **Phase 2: Android Bulk Interface**
    - [x] Add bulk order models to `ApiService.kt`
    - [x] Create `BulkDispatchScreen.kt` with dynamic row adding
    - [x] Integrate address selection per row
- [x] **Phase 3: Real-Time Batch Monitoring**
    - [x] Update `MerchantPortalScreen.kt` with "Create Batch" button
    - [x] Implement progress bar tracking for order batches
- [x] **Verification**
    - [x] Test multi-mission creation and automated wallet debiting
    - [x] Git automation (Commit and Push)
