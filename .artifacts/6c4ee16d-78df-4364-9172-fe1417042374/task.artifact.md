# Task: Fulfiller Conduct Policy Route & Order Acceptance Fix

- [ ] **1. Fulfiller Conduct Policy Route (`legalRoutes.js`, `legalController.js`, `app.js`, `MainActivity.kt`)**
    - [ ] Add `/terms/fulfiller` and `/legal/terms/fulfiller` routes in `legalRoutes.js`
    - [ ] Implement `getFulfillerTerms` in `legalController.js`
    - [ ] Mount `/terms` in `app.js`
    - [ ] Update `terms_viewer/{showFulfillerTerms}` in `MainActivity.kt` to point to `/legal/terms/fulfiller`
- [ ] **2. Fulfiller Mission Acceptance Fix (`orderController.js`, `ApiService.kt`, `FulfillerDashboardScreen.kt`)**
    - [ ] Return top-level `status` in `acceptOrder` response in `orderController.js`
    - [ ] Add `data: OrderResponseData?` to `OrderResponse` in `ApiService.kt`
    - [ ] Update `onAccept` handler in `FulfillerDashboardScreen.kt` to check `response.status ?: response.data?.status` and navigate to `active_order/$orderId`
    - [ ] Add "Active Mission in Progress" banner at top of `FulfillerDashboardScreen.kt`
- [ ] **Verification & Deployment**
    - [ ] Verify JS syntax using `node -c`
    - [ ] Build release App Bundle (`app-release.aab`)
    - [ ] Install release APK on connected Samsung Galaxy device
    - [ ] Commit and push all changes to GitHub
