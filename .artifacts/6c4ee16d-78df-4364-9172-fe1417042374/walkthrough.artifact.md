# Walkthrough: Distinct Onboarding per User Type

## Changes Made
1. **Upfront Selection Fixed**:
   - Modified `UserTypeSelectionScreen.kt` to present three distinct choices instead of two. Added the **"I want to Sell" (Merchant)** role.
   - Updated the navigation callbacks to emit explicit `CUSTOMER`, `FULFILLER`, and `MERCHANT` roles.
2. **De-coupled Onboarding Screens**:
   - I completely deleted the problematic, monolithic `SignupScreen.kt`.
   - Created `SignupCustomerScreen.kt`: Kept it extremely lightweight, perfectly suited for the customer profile.
   - Created `SignupFulfillerScreen.kt`: Separated it completely from Customer logic. Hardcoded the API role to `FULFILLER` and set up the foundation for specific sub-categories in upcoming prompts.
   - Created `SignupMerchantScreen.kt`: Separated it completely. Altered the UI labels slightly (e.g. "Contact Person Name", "Business Email") as a starting point. Hardcoded the API role to `MERCHANT`.
3. **Strict Navigation Enforcement**:
   - Stripped out the generic `composable("signup/{role}")` path in `MainActivity.kt`.
   - Added `composable("signup_customer")`, `composable("signup_fulfiller")`, and `composable("signup_merchant")` to guarantee that state, forms, and business logic can no longer mix across these user types.

## Build and Testing Status
- The Android project successfully compiled (`:app:assembleDebug`). The refactoring left no broken references or tangled states.
- All code has been successfully committed to version control, removing the old file, adding the new ones, and pushed to the `origin main` branch!