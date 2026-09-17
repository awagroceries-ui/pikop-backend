# Task: Fulfiller Incentives & Customer Loyalty Program

- [ ] **Infrastructure & Schema**
    - [ ] Create migration `1726560000000_growth_incentives.js`
- [ ] **Backend - Incentives & Loyalty**
    - [ ] Implement `STREAK_BONUS` and `PEAK_BONUS` logic in `walletService.js`
    - [ ] Implement customer `total_orders_completed` tracking
    - [ ] Harden `processReferralReward` with abuse prevention (IP/Phone checks)
- [ ] **Backend - Admin Config**
    - [ ] Update `adminController.js` and `settings.ejs` for new config keys
- [ ] **Android UI - Fulfiller App**
    - [ ] Update `ApiService.kt` DTOs for streak/peak data
    - [ ] Add Peak-Hour banner and Streak tracker to `FulfillerDashboardScreen.kt`
- [ ] **Verification**
    - [ ] Test streak bonus calculation
    - [ ] Test peak bonus application
    - [ ] Verify referral abuse blocking
    - [ ] Build and Deploy
