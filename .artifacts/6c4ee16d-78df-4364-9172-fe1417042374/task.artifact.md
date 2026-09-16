# Task: Nationwide-Ready Architecture

- [x] **Database & Schema**
    - [x] Create migration `1726510000000_nationwide_readiness.js`
- [x] **Backend Core - City Management**
    - [x] Update `adminController.js` (getCities, updateCityRules)
    - [x] Update `weatherService.js` (Dynamic city loop)
    - [x] Implement `expansionController.js` (Waitlist)
- [x] **Backend Core - Transaction Gating**
    - [x] Update `orderController.js` (`getQuote` logic for active cities)
- [x] **Android Integration**
    - [x] Update `ApiService.kt` (DTOs & Endpoints)
    - [x] Update `SignupFulfillerScreen.kt` (Dynamic Permit Rules)
    - [x] Update `StorefrontScreen.kt` (Dynamic Header & Coming Soon UI)
    - [x] Update `OrderQuoteScreen.kt` (Active city gating & Waitlist dialog)
- [ ] **Verification**
    - [ ] Test adding new city via Admin
    - [ ] Test gating in non-active city
    - [ ] Verify waitlist capture
    - [x] Build and Deploy
