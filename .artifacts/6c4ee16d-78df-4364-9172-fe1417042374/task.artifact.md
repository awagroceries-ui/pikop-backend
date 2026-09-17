# Task: In-App Account Deletion (Compliance)

- [ ] **Backend - Security & Gating**
    - [ ] Add `/confirm-password` to `authRoutes.js`
    - [ ] Implement `confirmPassword` in `authController.js`
    - [ ] Harden `deleteAccount` in `authController.js` (Checks for balance, missions, disputes)
- [ ] **Backend - Data Anonymization**
    - [ ] Implement irreversible anonymization logic in `deleteAccount`
    - [ ] Clean up `kyc_documents` and `user_fcm_tokens` on deletion
- [ ] **Android UI - Deletion Flow**
    - [ ] Update `ApiService.kt` with `confirmPassword` and hardened `deleteAccount` response
    - [ ] Redesign `DeleteAccount` dialog in `AccountScreen.kt` (Multi-step)
- [ ] **Verification**
    - [ ] Test blocking with active missions
    - [ ] Test blocking with non-zero wallet balance
    - [ ] Test successful deletion and PII cleanup
    - [ ] Build and Deploy
