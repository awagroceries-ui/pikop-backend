# Walkthrough - Business/Corporate Accounts (Phase 1)

I have successfully implemented the **Business/Corporate Account** system, enabling companies to centrally manage and pay for deliveries performed by their team members using a centralized wallet and per-user spending limits.

## Changes Made

### 🏢 1. Corporate account & Onboarding
- **New Account Type**: Added `CORPORATE` role to the system. This account type represents the company entity and acts as the "Payer" for missions.
- **Two-Step Onboarding**:
    - **Step 1**: Admin User creation (`SignupCorporateScreen.kt`).
    - **Step 2**: Business Details (Company name, CAC number, Headquarters, Billing email) (`CorporateBusinessSetupScreen.kt`).
- **Admin Verification**: All Corporate accounts enter a `PENDING_VERIFICATION` state for manual credential review by Pikop admins.

### 💰 2. Centralized Wallet Billing
- **Centralized Funding**: The company tops up its corporate wallet once. All authorized employees draw from this single balance, removing the need for individual reimbursements.
- **Spending Guards**: Corporate admins can set **Daily and Monthly spending limits** for every staff member.
- **Debit Logic**: Updated `walletService.js` to automatically verify sufficient company balance and enforce staff-specific limits before any corporate mission is activated.

### 👥 3. Staff Management & Authorization
- **Staff Authorization**: Corporate admins can invite employees by email. Once added, the employee's existing Pikop account (as a Customer) is linked to the company's billing profile.
- **Authorization Revocation**: Admins can revoke staff access or adjust their limits instantly from the dashboard.

### 📊 4. Corporate Dashboard (Console)
- **Spend Reporting**: Built a dedicated console in the Android app for corporate managers to:
    - Monitor their available billing balance.
    - Track total 30-day spend and active mission count.
    - View a list of "Top Spenders" within the company.
    - Manage the authorized staff list and their spending caps.

### 🛒 5. Integrated Checkout
- **Corporate Billing Option**: Authorized users now see a "Billing Method" selector at checkout. They can choose between **"Personal"** (Card/COD) and **"Corporate"** (Company Wallet).
- **Zero-Friction Activation**: Corporate orders are marked as **PAID** immediately upon creation (if within limits), bypassing the browser payment step for employees.

## Verification Results
- **Onboarding Flow**: [VERIFIED] Role selection correctly branches to Corporate setup.
- **Billing Integrity**: [VERIFIED] Corporate missions correctly debit the company wallet and log the authorized user in the ledger.
- **Limit Enforcement**: [VERIFIED] Attempting an order above a staff member's daily limit is correctly blocked with a clear error message.
- **Build Status**: [SUCCESS] Successfully compiled and verified the Android app.

## Deployment Instructions
To activate the corporate infrastructure on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
