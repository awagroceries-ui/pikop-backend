# Implementation Plan - Milestone 4: Viral Growth & Public Tracking

Introduce referral incentives, promotional discounts, and shareable real-time tracking links to drive growth and improve transparency.

## User Review Required

> [!IMPORTANT]
> - **Public Tracking**: Tracking links will be public (no login required). I will mask exact addresses (showing only zone names) and exclude sensitive item descriptions to maintain privacy.
> - **Referral Rewards**: Rewards are triggered **only** after the invited user completes their first successful delivery. This prevents "signup spam" and ensures high-quality growth.
> - **Promo Stacking**: By default, only one promo code can be applied per order.

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1723030000000_growth_and_tracking.js`
- **Orders**: Add `tracking_token` (uuid, unique), `promo_code_id` (uuid, FK), and `discount_amount` (numeric).
- **Promo Codes**: Create `promo_codes` table (id, code, discount_type, value, max_uses, used_count, valid_from, valid_to).
- **Redemptions**: Create `promo_code_redemptions` table to track usage per user.
- **Referrals**: Create `referral_rewards` table and add `referral_code` and `referred_by_user_id` to `users`.

---

### 2. Viral Growth Logic (Backend)

#### [NEW] `backend/src/controllers/promoController.js`
- `validateCode`: Checks validity, expiration, and user-specific usage limits.

#### [MODIFY] `backend/src/controllers/authController.js`
- Generate unique `referral_code` for new users.
- Link new users to referrers via optional `referral_code` during signup.

#### [MODIFY] `backend/src/services/walletService.js`
- Implement `triggerReferralReward(orderId)`: Detects first-time deliveries and credits both the referrer and referee.

---

### 3. Shareable Tracking (Backend & Web)

#### [NEW] `backend/src/controllers/trackingController.js`
- `getPublicTracking`: Returns limited, safe data for non-logged-in users.

#### [NEW] `backend/src/views/public_tracking.ejs`
- A lightweight, responsive web page with a live map (Google Maps JS) and status timeline.

#### [MODIFY] `backend/src/services/socketService.js`
- Add support for public tracking rooms (`tracking_{token}`).

---

### 4. Android UI Integration

#### [MODIFY] `OrderQuoteScreen.kt`
- Add **"Promo Code"** field with real-time validation and discount preview.

#### [MODIFY] `TrackOrderScreen.kt`
- Add **"Share Progress"** button that opens the native Android share sheet with the public tracking URL.

#### [MODIFY] `AccountScreen.kt`
- Display the user's **Referral Code** with a share button and a summary of earned rewards.

---

## Verification Plan

### Manual Verification
1.  **Tracking Share**: Create an order, tap "Share Progress" in the app, and open the resulting link in an incognito browser. Verify the map updates in real-time.
2.  **Promo Logic**: Create a promo code via DB, apply it in the app, and verify the `total_fare` is correctly reduced. Try using it twice with the same user to verify blocking.
3.  **Referral Reward**: Sign up a new "Referee" user using a "Referrer" code. Complete a delivery. Verify both wallets are credited with the configured reward amount.
4.  **Privacy Check**: Ensure the public tracking link **does not** reveal the recipient's phone number or exact house address.
