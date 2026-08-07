# Advanced Features Bundle Implementation

## Phase 1: Wallet & Withdrawal UI
- [ ] Backend: Implement `getWalletInfo` and `getTransactions`
- [ ] Backend: Create `walletRoutes.js`
- [ ] Android: Update `ApiService.kt` with wallet models and endpoints
- [ ] Android: Create `WalletScreen.kt`
- [ ] Android: Add "Wallet" navigation to Dashboards

## Phase 2: Fulfiller KYC Upload
- [ ] Backend: Setup `multer` and file storage
- [ ] Backend: Implement `uploadKYC` controller
- [ ] Android: Create `KycUploadScreen.kt` with image picking

## Phase 3: Order Cancellations & Fees
- [ ] Backend: Implement cancellation logic with fees
- [ ] Android: Add "Cancel Order" functionality to Tracking/Active screens

## Phase 4: Push Notifications (FCM)
- [ ] Backend: Integrate `firebase-admin` and token storage
- [ ] Android: Setup Firebase SDK and `PikopMessagingService`
