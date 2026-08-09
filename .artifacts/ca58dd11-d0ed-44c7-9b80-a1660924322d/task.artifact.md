# Final Stability & UI Refinement

## Phase 1: Build & Notifications
- [x] Fix 16 KB alignment build error (Manifest cleaned)
- [ ] Activate Chat Notifications in `PikopMessagingService.kt`
- [ ] Ensure FCM Token registration on every app start in `MainActivity.kt`

## Phase 2: Professional Bottom Navigation
- [ ] Create `MainAppScaffold.kt` (Unified navigation wrapper)
- [ ] Create `AccountScreen.kt` (Profile, Support, Logout)
- [ ] Integrate BottomBar in `MainActivity.kt`
- [ ] Refactor Dashboards to fit inside the new Scaffold

## Phase 3: Backend Robustness
- [ ] Add Didit fallback for dev environment in `diditService.js`
- [ ] Standardize 500/400 error payloads in `authController.js`

## Verification
- [ ] End-to-end Chat -> Notification test
- [ ] Full Logout -> Session Clear test
- [ ] Build success verification
