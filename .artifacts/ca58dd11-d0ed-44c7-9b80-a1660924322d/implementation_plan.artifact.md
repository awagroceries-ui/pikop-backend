# Implementation Plan - Role-Based Onboarding

Refactor the onboarding flow to allow users to choose their role (Customer or Fulfiller) at the beginning of their journey, providing a more intuitive and specialized experience.

## User Review Required

> [!IMPORTANT]
> - **Unified Account**: Every user will still have a unique email/phone, but their "Home" dashboard will be locked to their primary role choice during signup.
> - **Fulfiller Creation**: If a user signs up as a Fulfiller, the backend will automatically initialize their fulfiller profile and wallet in a single step.

## Proposed Changes

### 1. Backend & Database Refinement

#### [NEW] `backend/migrations/1722940000000_user_roles.js`
- Add `role` column to `users` table: `varchar(20)`, default `'CUSTOMER'`.

#### [MODIFY] `backend/src/controllers/authController.js`
- Update `signup`: Accept `role` from request. If `FULFILLER`, automatically create the record in the `fulfillers` table.
- Update `login`: Include `role` in the response payload and JWT.

---

### 2. Android: Entry Point Refactor

#### [NEW] `UserTypeSelectionScreen.kt`
- A premium, branded screen shown after Splash.
- Two large, high-impact cards:
    - **"I want to Send/Receive"** (Customer path)
    - **"I want to Deliver/Earn"** (Fulfiller path)
- Navigates to Signup/Login with the selected role context.

#### [MODIFY] `TokenManager.kt`
- Add `USER_ROLE_KEY` to store the chosen role locally for persistent navigation.

---

### 3. UI Path Specialization

#### [MODIFY] `SignupScreen.kt`
- Adapt titles and branding based on the chosen role (e.g., "Join the Fleet" for Fulfillers).

#### [MODIFY] `MainActivity.kt`
- Change `startDestination` to `user_type_selection` (if not logged in).
- Logic to automatically route logged-in users to `orders_dashboard` or `fulfiller_dashboard` based on their stored role.

---

## Verification Plan

### Manual Verification
1.  **Customer Path**: Select "Send", sign up, and verify you land on the **Customer Dashboard**.
2.  **Fulfiller Path**: Select "Deliver", sign up, and verify you land on the **Fulfiller Dashboard** with the KYC warning visible.
3.  **Persistence**: Close and reopen the app; verify it remembers your role and takes you back to the correct dashboard without asking for user type again.
