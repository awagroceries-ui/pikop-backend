# 📌 Task Checklist: Merchant Listing Save Retrofit Wildcard Fix

- `[/]` Task 1: Add Request Data Classes & Update Retrofit Service
  - `[ ]` Add `CreateProductRequest`, `CreateMenuItemRequest`, `CreateMerchantCouponRequest` in `ApiService.kt`
  - `[ ]` Update `addProduct`, `addMenuItem`, `createMerchantCoupon`, `updateMerchantSettings` in `ApiService.kt`

- `[ ]` Task 2: Update UI Screen Request Payloads
  - `[ ]` Update `AddEditProductScreen.kt` to use `CreateProductRequest` and `CreateMenuItemRequest`
  - `[ ]` Update `MerchantPortalScreen.kt` to use `CreateMerchantCouponRequest`

- `[ ]` Task 3: Build & Deploy to Connected Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 4: Git Automation
  - `[ ]` Stage, commit, and push changes to GitHub `main`
