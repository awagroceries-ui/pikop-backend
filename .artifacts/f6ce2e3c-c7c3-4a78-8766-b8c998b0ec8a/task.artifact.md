# Task List - Fix Cross-State Order Dispatch

- [ ] Backend: Create migration for `pickup_state` and `current_state` columns
- [ ] Backend: Update `orderController.js` to save and filter by state
- [ ] Backend: Update `fulfillerController.js` with radius and staleness checks
- [ ] Backend: Update `dispatchService.js` with hard state filter
- [ ] Android: Update `MapAddressSearchScreen.kt` to resolve state name
- [ ] Android: Update `Quote` and `Order` creation to send state
- [ ] Android: Implement periodic location + state pings in `FulfillerDashboardScreen.kt`
- [ ] Verification: Build Android app
- [ ] Verification: Git automation (Commit and Push)
