# Implementation Plan - Fix Change Password, Delete Account & Bank Editing

This plan addresses non-functional buttons for password changes and account deletion, and unlocks fulfiller banking details for editing.

## User Review Required

> [!IMPORTANT]
> **Account Deletion:** I will implement a "Soft Delete" approach where the user's status is set to `deleted` and their sensitive info (email/phone) is anonymized. This preserves referential integrity for existing missions and ledger entries while complying with data deletion requests.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **`changePassword` [NEW]**: Verify the current password, hash the new password, and update the database.
- **`deleteAccount` [NEW]**: Mark user status as `deleted`, invalidate all sessions, and anonymize email/phone fields.

#### [MODIFY] [authRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/authRoutes.js)
- Register `POST /change-password` and `POST /delete-account`.

#### [MODIFY] [settingsController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/settingsController.js)
- **`updateProfile`**: Extend to support `bank_name`, `account_number`, `bank_code`, and `account_name` for Fulfillers. This allows agents to update their payout details from the profile screen.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `changePassword(request: Map<String, String>)` and `deleteAccount()` methods.

#### [MODIFY] [ProfileEditScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/ProfileEditScreen.kt)
- Unlock `bankName` and `accountNumber` fields.
- Add an "Account Verification" flow (Bank Dropdown + Resolve Account) within the profile editor, similar to the one used in onboarding.

#### [NEW] [ChangePasswordDialog.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/ChangePasswordDialog.kt)
- Create a dialog to collect current password and new password with validation.

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- Wire the "Change Password" button to show the new dialog.
- Wire the "Delete Account" button to show a "Final Warning" confirmation dialog before calling the delete API.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Change Password:** Change the password, log out, and log back in with the new password.
2.  **Bank Update:** As a fulfiller, update the bank account in the profile and verify it reflects in the "Fulfiller Detail" view on the admin dashboard.
3.  **Delete Account:** Delete a test account and verify that login is no longer possible and data is anonymized in the DB.
