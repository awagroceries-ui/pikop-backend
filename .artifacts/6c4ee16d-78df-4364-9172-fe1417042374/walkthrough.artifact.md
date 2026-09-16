# Walkthrough - Unified Admin Dashboard for Fees & Commissions

I have successfully consolidated all platform fees and commission rates into a single, grouped admin management screen and ensured that all orders "freeze" their applicable rates at creation time for financial consistency.

## Changes Made

### 🛠️ Backend (Data Integrity & Storage)
- **Database Migration**: Created `1726480000000_unify_fees_and_commissions.js` which:
    - Added `dispatch_commission_amount` to the `orders` table.
    - Migrated the hardcoded Guest SMS charge into the `settings` table as `guest_sms_charge`.
- **Fee Freezing**: Updated all order creation paths (`orderController`, `paymentController`, and `commerceController`) to calculate and store the **Dispatch Commission (Pikop's 25% share)** at the time the mission is created.
- **Dynamic Costing**: Refactored the Guest SMS logic to pull the charge amount (default ₦50) from the database instead of using a hardcoded value.
- **Accurate Settlement**: Updated `walletService.js` to prioritize the "frozen" commission amount during mission settlement, ensuring that rate changes mid-mission don't affect existing earnings.

### 📊 Admin Dashboard (Consolidated Control)
- **Redesigned Settings Screen**: Grouped the six fee/commission types into intuitive sections for better management:
    - **Buyer-Borne Fees**: COD Protection Fee, Guest SMS Charge.
    - **Merchant-Borne Commissions**: Food, Groceries, Shop commissions.
    - **Fulfiller-Borne Commission**: Dispatch (Delivery) Commission.
- **Policy Transparency**: Added clear notices to the admin UI explaining that rate changes only apply to **new** orders, maintaining trust with users on active missions.
- **Enhanced Auditing**: The system now logs every change to these settings, including who made the change and what the new values are.

## Verification Results
- **Rate Freezing**: Confirmed that missions created before a rate change settle using their original rates.
- **Configurability**: Verified all 6 fee types can be edited from the single settings dashboard.
- **Backward Compatibility**: Ensured that legacy orders (missing the frozen commission column) still settle correctly using the platform's current default rate.

## Deployment Instructions
To apply these changes and seed the new settings to your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

> [!TIP]
> You can now adjust the Guest SMS charge or any Marketplace commission directly from the **Global Settings** page in the Admin Dashboard!
