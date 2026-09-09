# Walkthrough - Account Security & Bank Editing Fixes

I have fixed the non-functional "Change Password" and "Delete Account" buttons and unlocked fulfiller bank details for editing within the profile.

## Changes Made

### 1. Functional Account Security
- **Change Password:**
    - **Backend:** Implemented the `changePassword` endpoint in `authController.js` with current password verification.
    - **App:** Created a new `ChangePasswordDialog.kt` and wired it to the "Change Password" button in the `AccountScreen`. Customers and agents can now securely update their passwords.
- **Delete Account (Soft Delete):**
    - **Backend:** Implemented the `deleteAccount` endpoint. It marks the user as `deleted`, anonymizes their email/phone for privacy, and invalidates all active sessions.
    - **App:** Added a confirmation warning to the "Delete Account" button to prevent accidental data loss.

### 2. Unlocked Fulfiller Banking Details
- **The Problem:** Fulfillers were unable to update their bank payout details after onboarding, even if they entered incorrect information.
- **The Fix:**
    - **Refactored Profile Editor:** The `ProfileEditScreen.kt` now has an unlocked banking section for fulfillers.
    - **Integrated Verification:** Added the same bank selection and "Verify Account" logic used during onboarding directly into the profile editor. This ensures agents can update their bank but only with a valid, verified account name.
    - **Backend Support:** Updated the profile update API to persist `bank_name`, `account_number`, `bank_code`, and `account_name`.

### 3. Real-time UI Sync
- Updated the profile saving logic to refresh the local **TokenManager** immediately after a name or phone change. This ensures the "My Account" header updates without needing a restart.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these security and profile updates to your **VPS**:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **Change Password:** Go to "Account" -> "Change Password". Update your password and verify you can log back in with the new one.
2. **Bank Update (Agent):** Log in as a fulfiller. Go to "Account" -> "Edit Profile". Update your bank account, verify it, and save. Check that the new details appear in the Admin Dashboard.
3. **Delete Account:** Click "Delete Account" and confirm. Verify the app signs you out and you can no longer log in.
