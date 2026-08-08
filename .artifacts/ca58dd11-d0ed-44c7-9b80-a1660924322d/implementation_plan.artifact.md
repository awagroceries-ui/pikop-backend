# Implementation Plan - Final VPS Deployment & Synchronization

Synchronize the live VPS environment with all newly implemented features, including branding, wallet systems, KYC processing, and push notifications.

## User Review Required

> [!IMPORTANT]
> - **Sensitive Data**: You have provided the live keys. I have formatted them into a single configuration block below.
> - **Security**: Ensure that after copying these into your `.env` file, you do not share that file with anyone else.
> - **Firebase**: The Service Account JSON must be pasted as a single-line string in the `.env` file for the current code to read it correctly.

## Proposed Steps

### 1. Update `.env` on VPS
I have prepared the exact content for your `.env` file based on the keys you provided. This includes the database URL, JWT secrets, and the new service keys.

### 2. Database Migrations
Run the latest migrations to create the following tables:
- `saved_addresses`
- `fcm_tokens`
- `settings` (Platform commission)
- Update `quotes` with coordinates.

### 3. File System Setup
Create the `uploads` directory to handle Fulfiller KYC document storage.

### 4. Service Restart
Restart the `pikop-api` process using PM2 to apply all environment variables and code changes.

---

## Verification Plan

### Manual Verification
- **API Health**: Confirm `https://api.awa.name.ng/health` returns OK.
- **Admin Dashboard**: Log in and verify the "Withdrawals" and "Order Board" are visible.
- **Android App**:
    - Perform a fresh signup.
    - Test "Lagos" or "Abuja" address selection.
    - Upload a test KYC document and see it appear in the Admin queue.
