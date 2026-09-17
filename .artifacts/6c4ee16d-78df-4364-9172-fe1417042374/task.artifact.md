# Task: Item Insurance & Surge Pricing

- [x] **Infrastructure & Schema**
    - [x] Create migration `1726550000000_pricing_enhancements.js`
- [x] **Backend - Pricing Engine (`orderController.js`)**
    - [x] Implement demand ratio surge calculation in `getQuote`
    - [x] Implement insurance calculation in `getQuote`
    - [x] Update `createOrder` to persist insurance opt-in and surge values
- [x] **Backend - Wallet & Settings**
    - [x] Update `walletService.js` to handle `INSURANCE_PREMIUM` collection
    - [x] Update `settingsController.js` and `adminController.js` to manage new pricing toggles
- [x] **Android App - Checkout Enhancements (`OrderQuoteScreen.kt`)**
    - [x] Update `ApiService.kt` DTOs
    - [x] Add Surge Pricing visibility UI
    - [x] Add Item Protection Opt-In Checkbox
    - [x] Ensure billing total accurately reflects toggled insurance
- [x] **Verification**
    - [x] Trigger high demand, verify surge pricing in app
    - [x] Check opt-in box, verify premium is added to total
    - [x] Build and Deploy
