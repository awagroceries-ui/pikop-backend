# Task: Scheduled/Future-Dated Orders

- [ ] **Backend - Core Scheduling Logic**
    - [ ] Update `scheduledOrderJob.js` with 30-minute lead time activation
    - [ ] Update `orderController.js` with scheduling validation (7-day limit, security windows)
    - [ ] Update `commerceController.js` to support `scheduled_at` and merchant hour validation
    - [ ] Update `marketplaceController.js` to include `operating_hours` in API responses
- [ ] **Android - API Integration**
    - [ ] Update `ApiService.kt` DTOs (`CommerceOrderRequest`, `DiscoveryItem`)
- [ ] **Android - UI Implementation**
    - [ ] Implement Scheduling Toggle & Pickers in `OrderQuoteScreen.kt`
    - [ ] Implement Scheduling Toggle & Pickers in `CommerceCheckoutScreen.kt`
    - [ ] Add real-time validation for chosen times
- [ ] **Verification**
    - [ ] Verify background job activation
    - [ ] Verify merchant hour blocking
    - [ ] Build and Deploy
