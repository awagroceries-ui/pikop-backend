# Navigation, Stability & Notifications Fixes

- [/] 16 KB Page Size Fix
    - [ ] Update `app/build.gradle.kts` with legacy packaging
- [/] Push Notifications Activation
    - [ ] Update `PikopMessagingService.kt` to handle token refresh
    - [ ] Update `MainActivity.kt` to register token on app start
- [/] Navigation Hub & Logout
    - [ ] Create `NavigationDrawerContent.kt` shared component
    - [ ] Update `OrdersDashboardScreen.kt` with Drawer
    - [ ] Update `FulfillerDashboardScreen.kt` with Drawer
    - [ ] Update `TokenManager.kt` with full clear logic
- [/] Backend Stability
    - [ ] Add defensive logging to `authController.js`
