# Task List - Pikop Commerce Phase 4: Unified Checkout

- [ ] **Backend: Commerce Payment Integration**
    - [ ] Create `link_orders_to_commerce.js` migration
    - [ ] Implement `initializeCommerceOrder` in `commerceController.js`
    - [ ] Update `paymentController.js` webhook for commerce order creation
- [ ] **Android: ApiService Expansion**
    - [ ] Add `initializeCommerceOrder` Retrofit method and request/response models
- [ ] **Android: Checkout UI**
    - [ ] Create `CommerceCheckoutScreen.kt` with Order Summary and Address selection
    - [ ] Integrate automated price breakdown calculation
    - [ ] Connect Storefront clicks to Checkout
- [ ] **Verification**
    - [ ] Build and verify full purchase -> auto-dispatch flow
    - [ ] Git automation (Commit and Push)
