# Milestone 4: Viral Growth & Public Tracking Task List

## Phase 1: Database & Schema
- [ ] Create `growth_and_tracking` migration (`promo_codes`, `redemptions`, `referral_rewards`, `users` & `orders` update)

## Phase 2: Growth Logic (Backend)
- [ ] Update `authController.js` to generate referral codes
- [ ] Create `promoController.js` for code validation
- [ ] Update `orderController.js` to apply promos and generate tracking tokens
- [ ] Update `walletService.js` to handle referral payouts

## Phase 3: Public Tracking (Backend & Web)
- [ ] Create `trackingController.js` (Public API)
- [ ] Create `public_tracking.ejs` view
- [ ] Update `socketService.js` to support tracking rooms
- [ ] Register public routes in `app.js`

## Phase 4: Android App Integration
- [ ] Update `ApiService.kt` with promo and tracking endpoints
- [ ] Add Promo Code input to `OrderQuoteScreen.kt`
- [ ] Add "Share Tracking" to `TrackOrderScreen.kt`
- [ ] Update `AccountScreen.kt` with Referral Code & Rewards

## Verification
- [ ] Test public tracking link in browser
- [ ] Verify promo discount calculation
- [ ] Verify referral reward payout after first delivery
