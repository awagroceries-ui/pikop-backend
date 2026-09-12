# Implementation Plan - Fleet Performance & Automated Payouts

This module matures the operational side of Pikop by automating fulfiller payouts and providing agents with professional performance tracking tools.

## User Review Required

> [!IMPORTANT]
> **Instant Payout Threshold:** I am setting a default minimum withdrawal amount of **₦1,000**. This can be adjusted in the backend settings later.
>
> **Automatic Tiering:** Fulfiller tiers (Bronze, Silver, Gold) will now be automatically calculated based on their last 30 days of performance (Completion Rate and Avg Rating). High-tier agents will receive priority in the dispatch queue.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **`getProfile`**: Update to calculate and return "Active Tier" and "Month-to-Date Earnings."
- **`requestWithdrawal`**:
    - Check if the fulfiller has a `paystack_recipient_code`.
    - If not, use `paystackService` to create one using their bank details on file.
    - If the amount is below a certain "Instant" threshold, automatically call `paystackService.initiateTransfer`.

#### [NEW] [fleetJob.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/jobs/fleetJob.js)
- **Daily Performance Audit:** A job that runs every night to:
    1.  Calculate completion rates for all active agents.
    2.  Update tiers (e.g., Gold = >95% completion + >4.5 stars).
    3.  Flag underperforming agents for admin review.

---

### Android App

#### [MODIFY] [InsightsScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/InsightsScreen.kt)
- **Enhanced Stats:** Add cards for "Completion Rate %" and "Current Tier Status."
- **Earnings Graph:** Implement a simple bar chart (using Compose Canvas or a library if already present) to show earnings over the last 7 days.

#### [NEW] [feature/wallet] [WithdrawalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/wallet/WithdrawalScreen.kt)
- **Input:** Amount to withdraw.
- **Validation:** Ensure amount is within balance and above minimum threshold.
- **Confirmation:** Show linked bank account details for verification before submitting.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Register the `withdrawal` route.

---

## Verification Plan

### Manual Verification
1.  **Payout Flow:** As an agent with ₦5,000 balance, request a withdrawal of ₦2,000. Verify the backend creates a Paystack Transfer and the wallet is debited instantly.
2.  **Tier Test:** Manually update an agent's completion stats in the DB. Verify the "Insights" screen correctly reflects their new "Gold" or "Silver" status.
3.  **MTD Earnings:** Complete a mission. Verify the "Month-to-Date" earnings stat on the profile and insights screens updates correctly.
4.  **Security:** Try to withdraw more than the available balance. Verify the app and backend both block the transaction with a clear error.
