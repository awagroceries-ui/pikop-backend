# Task Checklist: Merchant COD Opt-In/Opt-Out

- `[/]` **Part 1: Database Migration**
  - `[ ]` Create and run migration to add `accepts_cod` to `vendors` and `kitchens`.
- `[ ]` **Part 2: Backend Logic Updates**
  - `[ ]` Update `setupMerchantProfile` in `merchantController.js`.
  - `[ ]` Update `getMerchantProfile` in `merchantController.js`.
  - `[ ]` Implement `updateMerchantSettings` in `merchantController.js`.
  - `[ ]` Update `getDiscovery` in `commerceController.js` to return `accepts_cod`.
  - `[ ]` Register routes in `merchantRoutes.js`.
- `[ ]` **Part 3: Android API Layer**
  - `[ ]` Update `SetupMerchantRequest`, `MerchantProfile`, and `DiscoveryItem` in `ApiService.kt`.
  - `[ ]` Add `updateMerchantSettings` to `ApiService` interface.
- `[ ]` **Part 4: Android UI Layer**
  - `[ ]` Add COD toggle to `MerchantBusinessSetupScreen.kt`.
  - `[ ]` Update `CommerceCheckoutScreen.kt` to enforce `accepts_cod`.
  - `[ ]` Add Settings tab with COD toggle to `MerchantPortalScreen.kt`.
- `[ ]` **Part 5: Verification & Deployment**
  - `[ ]` Build and test the Android client.
  - `[ ]` Git commit and push changes.
