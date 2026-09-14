# Walkthrough: Merchant Onboarding (Contact Person + Business Verification)

## Changes Made
1. **Stage 1 (Contact Person)**:
   - Modified `SignupMerchantScreen.kt` to clearly ask for "Contact Person Details" instead of mixing business info. It collects Name, Role (Owner/Manager), Email, and Phone.
   - Upon OTP verification, `MainActivity.kt` now routes `MERCHANT` roles directly to Stage 2 (`merchant_business_setup`).
2. **Stage 2 (Business Details)**:
   - Created a brand new screen: `MerchantBusinessSetupScreen.kt`.
   - Collects Business Name, Category (Food/Groceries/Shop), Address, Bank Details, and CAC Number.
   - **Conditional NAFDAC**: A dynamic NAFDAC Number field appears *only* if the category is "Food" or "Groceries". If "Shop" is selected, it hides.
3. **Backend Support (`merchantController.js`)**:
   - Added a new endpoint (`POST /api/v1/merchants/setup`) that inserts the business into the appropriate table (`kitchens` or `vendors`) based on the selected category.
   - Automatically sets the status to `pending_business_verification`.
   - **KYC Pipeline Integration**: CAC and NAFDAC strings are inserted directly into the `kyc_documents` table (`doc_type='CAC'` or `doc_type='NAFDAC'`) for manual admin dashboard review, bypassing the Prembly identity provider seamlessly!
4. **Product-Level NAFDAC**:
   - As requested, I investigated the product creation flow (`AddEditProductScreen.kt` and `marketplaceController.js`). Good news: **this functionality was already built in seamlessly!** The `nafdac_number` property is already an optional field natively supported when merchants create/edit a generic marketplace product!

## Build and Testing Status
- The Android project logic compiles flawlessly (`:app:assembleDebug`).
- Changes are fully committed and pushed to your `main` repository!