# Implementation Plan - Growth & Engagement (Loyalty & Referrals)

This plan finalizes the Pikop Growth Engine by enabling loyalty point redemption and providing users with a clear view of their referral performance.

## Proposed Changes

### 1. Backend Logic (Node.js)

#### [MODIFY] [growthController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/growthController.js)
- Implement `redeemPoints`:
    - Checks if the user has at least 500 points.
    - Rate: 1 Point = ₦1.
    - Transaction: Atomicly deducts points from `loyalty_ledger` and credits the user's wallet.
- Implement `getReferralHistory`:
    - Returns a list of users referred by the current user.
    - Includes: Name (masked for privacy, e.g. "John D."), status (Joined / Completed First Order), and reward status.

#### [MODIFY] [growthRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/growthRoutes.js)
- Register `POST /redeem` and `GET /referrals`.

### 2. Android App Integration (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `RedeemRequest` and `ReferralItem` DTOs.
- Add `redeemPoints()` and `getReferralHistory()` endpoints.

#### [MODIFY] [GrowthRewardsScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/growth/GrowthRewardsScreen.kt)
- **Redemption**: Add a "Redeem" button that appears when points ≥ 500. Show a confirmation dialog explaining the points-to-wallet conversion.
- **Referral List**: Add a new section "My Referred Friends" showing the status of people who used the user's code.

## User Review Required

> [!NOTE]
> **Redemption Rate**
> I am setting the redemption rate to **1 Point = ₦1** with a **500 point minimum**. This means a user can redeem ₦500 once they've spent ₦50,000 on the platform. Let me know if you want these values adjusted.

> [!IMPORTANT]
> **Privacy in Referrals**
> In the referral history list, I will only show the first name and last initial of the referred person to maintain privacy while still giving the user enough info to know who they are.

## Verification Plan

### Manual Verification
1.  **Point Earning**: Complete a mission of ₦2,000. Verify the user's loyalty balance increases by 20 points.
2.  **Redemption**: Top up points to 500. Click "Redeem". Verify 500 points are deducted and ₦500 is added to the wallet balance.
3.  **Referral Tracking**: Use a referral code on Signup. Verify the referrer's dashboard shows the new user as "Joined". Complete a mission and verify the status changes to "Completed" and the ₦250 bonus is paid out.
