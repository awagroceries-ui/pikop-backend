# Implementation Plan: Merchant Onboarding (Contact Person + Business Verification)

## 🔍 Diagnostic Report

I have investigated the backend and current frontend setups:
1. **Current Merchant Role**: `MERCHANT` is accepted in the auth signup endpoint, but it stops there. The user gets a basic `users` record.
2. **Business Separation**: The `MerchantPortalScreen.kt` and `merchantController.js` already exist and assume a `MerchantProfile` object. The onboarding flow needs to formally bridge a new user to becoming a fully provisioned Merchant Profile.
3. **NAFDAC / CAC Verification**: The KYC structure we discussed in the previous prompt (`uploadDocument` into `kyc_documents`) applies perfectly here. Business documents (CAC, NAFDAC) will be uploaded as `doc_type='CAC'` or `doc_type='NAFDAC'` for manual admin review.
4. **Approval States**: The prompt requires distinct statuses (`pending_contact_verification` -> `pending_business_verification` -> `active`). The merchant profile will need an explicit `status` field to lock out product listing until `active`.

## User Review Required

> [!IMPORTANT]
> **Two-Stage Onboarding Architecture**
> The prompt calls for two stages. I will implement this as follows:
>
> 1. **Stage 1 (Contact Person)**: Handled by `SignupMerchantScreen.kt`. Collects the owner's Name, Email, Phone, and Password, creating the `users` account with role `MERCHANT`.
> 2. **Stage 2 (Business Verification)**: After verifying their email OTP, they land on a new screen: `MerchantBusinessSetupScreen.kt`. This collects Business Name, Category, Address, CAC, NAFDAC (conditional on consumables), and Bank Details, then pushes to a new backend endpoint `createMerchantProfile` to spawn the actual business entity and lock it into `pending_business_verification`.
>
> *Note on Prembly:* Since the core identity KYC (Prembly/Dojah) is handled universally via the Profile/Wallet screens across the app, Stage 1 will focus on creating the account. Identity verification will be flagged as a prerequisite during the Admin Review phase of the business docs.
>
> Do you approve of this architecture?

## Proposed Changes

### Part 1: Android Frontend

#### [MODIFY] [SignupMerchantScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupMerchantScreen.kt)
- Refine labels: "Contact Person Name", "Role at Business" (Owner/Manager).
- This handles the core account creation (Stage 1).

#### [NEW] [MerchantBusinessSetupScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/MerchantBusinessSetupScreen.kt)
- Create this new screen for Stage 2.
- Fields: Business Name, Category (Dropdown: Food/Groceries/Shop), Business Address, CAC Number.
- Conditional NAFDAC: If Category == Food or Groceries, show optional NAFDAC Number input.
- Bank Account Details.
- Submits to `createMerchantProfile`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add route `composable("merchant_business_setup")`.
- Update the email OTP success flow: if `userRole == "MERCHANT"`, check if they have a business profile. If not, route to `merchant_business_setup` instead of `main` or `terms`.

### Part 2: Backend Logic

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Add `createMerchantProfile` endpoint.
- Accepts business details, creates the record with `status = 'pending_business_verification'`.
- Modifies `getMerchantDashboard` and portal routes to block interactions (like adding products) if `status != 'active'`.

## Verification Plan

### Manual Verification
1. Sign up via Merchant route.
2. Complete Stage 1 (Contact Details) -> Receive OTP.
3. Verify OTP -> Ensure it routes to `merchant_business_setup` (Stage 2) instead of Home.
4. Test NAFDAC logic: Select "Shop", NAFDAC disappears. Select "Food", NAFDAC appears.
5. Submit Stage 2 -> Verify backend creates profile with `pending_business_verification` status.
6. Attempt to add a product (if portal allows entry) -> Verify access is blocked/restricted until active.