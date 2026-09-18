# Task: Fix Merchant Product Creation

- [x] **Backend Repairs**
    - [x] Update `getMerchantProfile` in `merchantController.js` to return all fields
    - [x] Add status checks to `addProduct` in `marketplaceController.js`
    - [x] Add status checks to `addMenuItem` in `kitchenController.js`
- [x] **Android API & Models**
    - [x] Update `MerchantProfile` DTO in `ApiService.kt`
- [x] **Android UI Fixes**
    - [x] Update logic in `AccountScreen.kt` for legacy merchant repair
    - [x] Enhance `MerchantPortalScreen.kt` with "Complete Setup" CTA and status banners
    - [x] Gate "Add Item" FAB based on active status
- [ ] **Verification**
    - [ ] Verify legacy merchant routing to setup
    - [ ] Verify pending verification banner
    - [ ] Verify successful item creation for active merchants
    - [ ] Build and Deploy
