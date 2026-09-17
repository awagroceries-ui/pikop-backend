# Task: In-App Wallet Checkout

- [ ] **Backend - Wallet Logic**
    - [ ] Implement `processIndividualWalletPayment` in `walletService.js`
    - [ ] Update `orderController.js` to handle `payment_method: 'wallet'`
    - [ ] Update `commerceController.js` to handle `payment_method: 'wallet'`
- [ ] **Android - Checkout Integration**
    - [ ] Update `OrderQuoteScreen.kt` (Fetch balance, show Wallet option)
    - [ ] Update `CommerceCheckoutScreen.kt` (Add Wallet payment card)
- [ ] **Verification**
    - [ ] Test successful wallet checkout
    - [ ] Test insufficient balance rejection
    - [ ] Build and Deploy
