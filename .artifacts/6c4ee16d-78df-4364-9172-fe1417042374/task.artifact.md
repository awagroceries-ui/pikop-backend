# Task Checklist: Distinct Onboarding per User Type

- `[/]` **Part 1: Upfront Role Selection**
  - `[ ]` Modify `UserTypeSelectionScreen.kt` to include 3 options (Customer, Fulfiller, Merchant).
- `[ ]` **Part 2: Separate Onboarding Flows**
  - `[ ]` Create `SignupCustomerScreen.kt` (from original SignupScreen logic).
  - `[ ]` Create `SignupFulfillerScreen.kt` (Base structure, hardcoding `FULFILLER`).
  - `[ ]` Create `SignupMerchantScreen.kt` (Base structure, hardcoding `MERCHANT`).
  - `[ ]` Remove monolithic `SignupScreen.kt`.
- `[ ]` **Part 3: Navigation Routing**
  - `[ ]` Update `MainActivity.kt` to route explicitly to `signup_customer`, `signup_fulfiller`, and `signup_merchant`.
- `[ ]` **Part 4: Verification & Git**
  - `[ ]` Build and verify logic compiles.
  - `[ ]` Git commit and push changes.