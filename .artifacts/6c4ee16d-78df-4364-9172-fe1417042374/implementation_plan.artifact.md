# Implementation Plan - In-App Account Deletion (Compliance)

This plan implements a secure, multi-step account deletion flow for all users (Customers, Fulfillers, Merchants, Fleet Partners) to comply with Google Play requirements and NDPA regulations.

## 🔍 Diagnostic Summary
- **Current State**: A basic soft-delete exists in `authController.js` but lacks security checks (active missions, wallet balance) and thorough data anonymization.
- **Compliance Requirement**: Users must be able to delete their account from within the app. Identification data must be removed/anonymized, but transactional data should be retained for accounting/legal reasons in an anonymized form.

## Proposed Changes

### 1. Backend Enhancements (Node.js)

#### [MODIFY] [authRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/authRoutes.js)
- Add `POST /confirm-password` endpoint to verify identity before sensitive actions.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **Implement `confirmPassword`**: Standard bcrypt verification.
- **Harden `deleteAccount`**:
    - **Pre-deletion Checks**:
        - Block if there are **active missions** (Status not in `DELIVERED`, `CANCELLED`, `RELEASED`, `REFUNDED`).
        - Block if **wallet balance** (available or pending) is non-zero.
        - Block if there are **unresolved disputes** (`status` is `OPEN` or `INVESTIGATING`).
    - **Data Anonymization**:
        - `users` table: Set `full_name = 'Deleted User'`, `email = 'deleted_' || id || '@pikop.ng'`, `phone = 'deleted_' || id`, `password_hash = '*'`.
    - **Data Deletion**:
        - Delete entries in `kyc_documents`, `user_fcm_tokens`.
        - Revoke all `user_sessions`.
        - Delete profile photo from local storage (if applicable).
    - **Role Specifics**: Handle linked `fulfillers`, `vendors`, `kitchens`, or `fleet_partners` by setting their status to `deleted` or `suspended`.

### 2. Android UI Refinement (Compose)

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- Redesign the **Delete Account** flow:
    1.  **Initial Warning**: Explain consequences (irreversible, loss of wallet access).
    2.  **Identity Verification**: Prompt for password re-entry.
    3.  **Final Confirmation**: A last "Are you sure?" check.
- Display detailed error messages if deletion is blocked (e.g., "You have 2 active missions. Please complete them before deleting your account").

## Verification Plan

### Manual Verification
1.  **Block Test (Active Mission)**: Create a mission. Attempt deletion. Verify the app displays a clear error and prevents deletion.
2.  **Block Test (Wallet Balance)**: Top up wallet with ₦100. Attempt deletion. Verify the app prompts the user to withdraw or spend the balance first.
3.  **Success Test**: Delete an empty/inactive account.
    - Verify redirection to the login/landing screen.
    - Verify database anonymization (Email/Phone/Name changed).
    - Verify KYC documents and sessions are removed.
    - Attempt login with old credentials. Verify it fails.
4.  **Retention Test**: Check an old order for the deleted user. Verify the "Deleted User" label appears but mission details are preserved.
