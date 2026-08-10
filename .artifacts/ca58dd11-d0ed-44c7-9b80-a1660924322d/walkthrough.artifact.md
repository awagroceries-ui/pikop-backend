# Walkthrough - Milestone 3: Corporate/SME Infrastructure

I have successfully implemented the **Corporate/SME Infrastructure**, enabling businesses to manage high-volume delivery operations with centralized billing and automated reporting.

## Changes Made

### 1. High-Value Billing Infrastructure (Backend)
- **Corporate Account Hierarchy**: Implemented a robust sub-account system.
    - **Billing Admins**: Can manage payment mandates, top up wallets, and invite staff.
    - **Staff Members**: Can create orders that are automatically billed to the company account.
- **Dual Billing Support**:
    - **Prepaid Wallet**: Companies can top up a centralized balance.
    - **Direct Debit (Mandates)**: Integrated Paystack's recurring mandate logic for real-time charges per order, providing a seamless "monthly billing" feel with zero debt risk.
- **Automated Invoicing Engine**: Created a background job that generates itemized monthly PDF summaries, providing companies with a clear audit trail of their staff's delivery activity.

### 2. Streamlined Checkout Experience (Android)
- **Billing Selector**: Updated the `OrderQuoteScreen` with a dynamic **Billing Method** selector.
- **Corporate Visibility**: If a user is linked to a corporate account, they can now tap their company's name to instantly pay using the corporate mandate or wallet, bypassing the individual card payment screen.
- **Unified Error Parsing**: Implemented professional error handling that translates complex server messages (like "Insufficient corporate funds") into clear user guidance.

### 3. Management Command Center (Dashboard)
- **Corporate Directory**: Added a new management screen to the Admin Dashboard for monitoring business partners.
- **Operational Controls**: Ops staff can now view staff counts, billing types, and manually suspend accounts in the event of mandate failures.

## Verification Results

### Automated Build
- Ran `./gradlew :app:assembleDebug` and the build finished successfully.

### Manual Logic Verified
- **Transaction Safety**: Verified that orders are only created if the corporate wallet has a sufficient balance or a valid mandate exists.
- **Permission Gating**: Confirmed that only "Billing Admins" can access the staff invitation and financial reporting endpoints.

## Deployment Instructions (VPS)
Run these commands in your VPS terminal to enable Corporate Billing:

```bash
# 1. Update code and apply corporate schema
cd /var/www/pikop-api
git pull origin main
cd backend && npm run migrate:up

# 2. Restart the engine
pm2 restart pikop-api
```

> [!TIP]
> To test the flow, create a corporate account via the API, then use the **Menu** tab in the app to switch your billing method during a delivery request.
