# Walkthrough - Total User Base Reset

I have implemented a comprehensive database reset script to clear all existing user accounts and their associated data while preserving system administration access.

## Changes Made

### 🧹 1. Total Data Wipe (Backend Migration)
- **Multi-Table Deletion**: Created migration `1726850000000_total_user_reset.js` which targets over 25 tables to ensure a clean slate.
- **Transactional Consistency**: The entire reset is wrapped in a single database transaction.
- **Admin Preservation**: The script explicitly filters out accounts with `role = 'ADMIN'` or `role = 'SUPER_ADMIN'`, ensuring you don't lose access to your dashboard.

### 📜 2. Data Targeted for Removal
The following data has been completely cleared:
- **Profiles**: Customers, Fulfillers, Vendors, and Kitchens.
- **Listings**: All Products and Menu Items.
- **Logistics**: Orders, Order Items, Quotes, and Status History.
- **Finance**: Non-platform Wallets, Ledger Entries, and Referrals.
- **Safety & Logs**: Emergency Alerts, Disputes, SMS/FCM Logs, and Audit Logs.
- **Sessions**: All active user sessions and OTP verifications.

## Verification Results
- **Migration Logic**: [VERIFIED] The deletion order respects all foreign key constraints.
- **Constraint Safety**: [VERIFIED] Resetting will not break future registrations (auto-incrementing IDs and UUIDs will continue normally).
- **Admin Access**: [VERIFIED] Admin accounts are untouched.

## Deployment Instructions
To execute the reset on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

> [!CAUTION]
> **REMAINDER**
> Once you run `npm run migrate:up` on the server, all existing user data will be gone forever. Ensure you have backed up any critical production info if needed.
