# Task Checklist: Merchant Onboarding & NAFDAC

- `[/]` **Part 1: Stage 1 (Contact Person)**
  - `[ ]` Update `SignupMerchantScreen.kt` (Refine labels to Contact Person, Role, etc.).
- `[ ]` **Part 2: Stage 2 (Business Verification)**
  - `[ ]` Create `MerchantBusinessSetupScreen.kt` (Business Name, Category, CAC, conditional NAFDAC, Bank details).
  - `[ ]` Update `MainActivity.kt` to route merchants from OTP to `merchant_business_setup`.
- `[ ]` **Part 3: Backend Logic**
  - `[ ]` Add `setupMerchantProfile` in `merchantController.js`.
  - `[ ]` Add route in `merchantRoutes.js`.
  - `[ ]` Update `addProduct` and `updateProduct` in `merchantController.js` to accept `nafdac_number`.
- `[ ]` **Part 4: Product-level NAFDAC (User Addition)**
  - `[ ]` Update `ApiService.kt` to support `nafdac_number` in `ProductRequest`.
  - `[ ]` Update `AddEditProductScreen.kt` to include an optional NAFDAC field.
- `[ ]` **Part 5: Verification & Git**
  - `[ ]` Build and verify logic compiles.
  - `[ ]` Git commit and push changes.