# Milestone 5: Full Account Hub & Platform Polish Task List

## Phase 1: Database & Schema
- [ ] Create `settings_and_sessions` migration (`user_sessions`, `saved_recipients`, `users` & `fulfillers` update)

## Phase 2: Full Settings Logic (Backend)
- [ ] Create `settingsController.js` (Profile update, recipient CRUD, session management, account deletion)
- [ ] Create `settingsRoutes.js`
- [ ] Update `authController.js` to register/track sessions

## Phase 3: Payment Polish (Prompt 9)
- [ ] Update `CheckoutHelper.kt` to remove channel restrictions
- [ ] Update `paymentController.js` to capture channel in webhook

## Phase 4: Android UI Refinement
- [ ] Create `ProfileEditScreen.kt`
- [ ] Create `NotificationSettingsScreen.kt`
- [ ] Create `RecipientManagementScreen.kt`
- [ ] Update `AccountScreen.kt` with full settings list
- [ ] Update `MainAppScaffold.kt` navigation

## Verification
- [ ] Verify multi-channel Paystack payment (USSD/Transfer)
- [ ] Verify remote session logout
- [ ] Verify "Saved Recipients" shortcut in order flow
