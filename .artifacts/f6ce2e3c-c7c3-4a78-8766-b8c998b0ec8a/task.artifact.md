# Task List - Fix Order Activation, Fulfiller History & Rating System

- [x] Backend: Centralize `activatePaidMission` in `paymentController.js`
- [x] Backend: Match SQL fields and values in order insertion (Fixes 500/Missing Orders)
- [x] Backend: Update `getAvailableOffers` to include `PAYMENT_CAPTURED`
- [x] Backend: Update `acceptOrder` status check to include `PAYMENT_CAPTURED`
- [x] Backend: Create migration for customer rating columns
- [x] Backend: Implement `rateFulfiller` controller and route
- [x] Android: Add `rateFulfiller` to `ApiService.kt`
- [x] Android: Add "RESUME" button to `FulfillerOrdersScreen.kt`
- [x] Android: Implement `RatingDialog` in `TrackOrderScreen.kt`
- [ ] Verification: Build Android app
- [ ] Verification: Git automation (Commit and Push)
