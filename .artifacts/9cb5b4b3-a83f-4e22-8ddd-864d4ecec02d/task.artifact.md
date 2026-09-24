# 📌 Task Checklist: Merchant Operating Hours Implementation

- `[/]` Task 1: Backend Controller Update (`merchantController.js`)
  - `[ ]` Update `updateMerchantProfile` in `merchantController.js` to accept `operating_hours`
  - `[ ]` Ensure JSON serialization & NULL safety for `operating_hours` in `vendors` and `kitchens` tables

- `[ ]` Task 2: Android App Merchant Settings UI (`MerchantPortalScreen.kt`)
  - `[ ]` Add Operating Hours configuration card (Opening & Closing Time selectors) in `SettingsTabContent`
  - `[ ]` Update `onUpdateSettings` handler to pass `operating_hours` in `updateMerchantSettings`

- `[ ]` Task 3: Customer Storefront Display
  - `[ ]` Update `StorefrontScreen.kt` & `ShopStorefrontScreen.kt` to display store Operating Hours badge

- `[ ]` Task 4: Device Deployment & Git Automation
  - `[ ]` Build debug APK (`app:assembleDebug`) and deploy to connected device (`192.168.1.2:42447`)
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
