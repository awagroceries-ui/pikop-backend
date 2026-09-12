# Task List - Strict Policy Adjustments

- [x] **Backend: Policy Enforcement**
    - [x] Update `initiateReturn` in `orderController.js` to 75% fee
    - [x] Update `cancelOrder` in `orderController.js` with 25% fee and post-pickup restriction
    - [x] Update `legalController.js` with new terms and `/legal/config` endpoint
- [x] **Android: App Alignment**
    - [x] Update `ApiService.kt` for legal config
    - [x] Update `TermsScreen.kt` to fetch and render live terms
    - [x] Update `TrackOrderScreen.kt` cancellation logic
- [x] **Verification**
    - [x] Build and verify policy triggers
    - [x] Git automation (Commit and Push)
