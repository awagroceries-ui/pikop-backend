# Implementation Plan - Fulfiller Incentives & Customer Loyalty

This plan implements performance-based Fulfiller incentives and formalizes the Customer loyalty and referral program.

## Proposed Changes

### 1. Database & Schema Enhancements

#### [NEW] [growth_incentives migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726560000000_growth_incentives.js)
- **Extend `users`**:
    - `total_orders_completed`: (INTEGER, default 0) - For simple loyalty tiering.
- **Extend `fulfillers`**:
    - `current_streak_days`: (INTEGER, default 0)
    - `last_streak_date`: (DATE)
- **Update `wallet_ledger_entries`**: Add `STREAK_BONUS` and `PEAK_BONUS` to the purpose check constraint.
- **Seed Settings**:
    - `streak_bonus_7_day`: '1000'
    - `streak_bonus_30_day`: '5000'
    - `peak_hour_start`: '16:00'
    - `peak_hour_end`: '19:00'
    - `peak_hour_bonus`: '300'

### 2. Backend Logic (Node.js)

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- **`processMissionSettlement`**:
    - **Fulfiller Streak Check**:
        - Check `last_streak_date`. If it was yesterday, increment `current_streak_days`. If older, reset to 1.
        - If `current_streak_days` hits 7 or 30, award the configured `STREAK_BONUS` from the platform wallet to the fulfiller.
    - **Peak-Hour Bonus**:
        - Check if the order was created/matched during the `peak_hour_start` - `peak_hour_end` window.
        - If yes, award `PEAK_BONUS` from the platform wallet to the fulfiller.
    - **Customer Loyalty**:
        - Increment `total_orders_completed` for the user.
        - (Optional) If it hits a milestone (e.g., 10th order), award a loyalty point multiplier or flat bonus.
- **Referral Abuse Prevention**:
    - Update `processReferralReward` to verify that the new user does not share the same device fingerprint (IP address or phone number) as the referrer.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update settings management to include the new streak and peak hour configuration fields.

### 3. Android Frontend (Compose)

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- **Peak Hour Alert**: Display a prominent banner at the top of the dashboard if a peak-hour bonus is currently active (e.g., "Peak Bonus Active! Earn an extra ₦300 per delivery until 7 PM").
- **Streak Tracker**: Add a visual element (e.g., a flame icon) showing the current streak count and progress toward the next bonus.

## Verification Plan

### Manual Verification
1.  **Fulfiller Streak**: Manually advance the server date or trigger 7 consecutive orders. Verify the `STREAK_BONUS` is credited to the wallet.
2.  **Peak Hour Bonus**: Place an order during the configured peak window. Verify the fulfiller receives the base pay + `PEAK_BONUS`.
3.  **Referral Abuse**: Attempt to sign up a new user using the same IP address as an existing referrer. Complete an order. Verify the referral bonus is skipped.
