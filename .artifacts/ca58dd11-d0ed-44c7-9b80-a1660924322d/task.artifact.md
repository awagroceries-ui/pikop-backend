# Role-Based Onboarding Implementation [DONE]

## Backend & Database
- [x] Create `user_roles` migration
- [x] Update `authController.js` (Role-based signup/login)
- [x] Update `authService.js` (JWT role payload)

## Android: Data Layer
- [x] Update `TokenManager.kt` to store `user_role`
- [x] Update `ApiService.kt` (SignupRequest and AuthResponse models)

## Android: UI Layer
- [x] Create `UserTypeSelectionScreen.kt`
- [x] Refactor `SignupScreen.kt` (Role-aware UI)
- [x] Refactor `MainActivity.kt` (Role-based routing logic)

## Verification
- [ ] Test Customer signup flow
- [ ] Test Fulfiller signup flow
- [ ] Verify persistence after app restart
