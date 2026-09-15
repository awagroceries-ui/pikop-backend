# Walkthrough - Fix Merchant Signup Database Constraint

I have addressed the "value too long for type character varying(20)" error that was blocking Merchant signups during the business verification step.

## Changes Made

### 1. Database Schema Update
- Created migration `1726460000000_extend_status_column_lengths.js`.
- Increased the length of the `status` column from **20 to 50 characters** across four key tables:
    - `vendors`
    - `kitchens`
    - `users`
    - `fulfillers`
- This ensures that descriptive statuses like `pending_business_verification` (29 characters) can be safely stored without triggering database errors.

## Deployment Instructions

To apply this fix to your production environment, please run the following commands on your VPS:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

> [!IMPORTANT]
> The signup will continue to fail on the server until the `npm run migrate:up` command is executed to update the database schema.
