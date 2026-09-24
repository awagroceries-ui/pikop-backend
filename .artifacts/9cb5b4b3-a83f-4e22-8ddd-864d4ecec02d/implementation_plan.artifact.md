# 📋 Implementation Plan: Merchant Listing Save Retrofit Wildcard Type Fix

Fix the "failed to save: parameter type must not include a type variable or wildcard" exception when creating new merchant items (products / menu items) or coupons.

---

## 🔍 Root Cause Analysis

In `ApiService.kt` (Android app), Retrofit endpoint declarations for creating products, menu items, coupons, and updating merchant settings used generic un-typed body maps:
```kotlin
@POST("api/v1/marketplace/products")
suspend fun addProduct(@Body request: Map<String, Any>): Map<String, Any>

@POST("api/v1/kitchens/menu-items")
suspend fun addMenuItem(@Body request: Map<String, Any>): Map<String, Any>
```
In Kotlin JVM bytecode, `@Body request: Map<String, Any>` compiles to `java.util.Map<java.lang.String, ? extends java.lang.Object>`, which contains a wildcard (`?`).

When Retrofit reflects on parameter types during method initialization, Retrofit strictly prohibits wildcards in `@Body` parameters and throws:
`java.lang.IllegalArgumentException: Parameter type must not include a type variable or wildcard: java.util.Map<java.lang.String, ?>`

This causes the "Save Listing" action on `AddEditProductScreen.kt` to throw an exception when tapping "Save Listing".

---

## 🛠️ Proposed Changes

### Android App (`:app`)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add strongly-typed Kotlin request data classes:
  - `CreateProductRequest`
  - `CreateMenuItemRequest`
  - `CreateMerchantCouponRequest`
- Update Retrofit service method parameters:
  - `addProduct(@Body request: CreateProductRequest)`
  - `addMenuItem(@Body request: CreateMenuItemRequest)`
  - `createMerchantCoupon(@Body request: CreateMerchantCouponRequest)`
  - `updateMerchantSettings(@Body request: @JvmSuppressWildcards Map<String, Any>)`

#### [MODIFY] [AddEditProductScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/AddEditProductScreen.kt)
- Update "Save Listing" click handler to instantiate and pass `CreateProductRequest` (for vendors) or `CreateMenuItemRequest` (for kitchens) instead of untyped `Map<String, Any>`.

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Update coupon creation handler to instantiate and pass `CreateMerchantCouponRequest`.

---

## 🧪 Verification Plan

### Automated & Device Verification
1. Compile `:app` debug build (`gradle_build("app:assembleDebug")`).
2. Deploy APK to connected device.
3. Open Merchant Portal -> Add Item screen (`AddEditProductScreen.kt`).
4. Fill item name, price, description, category, and tap **"Save Listing"**.
5. Verify item saves successfully with status Toast "Item Saved Successfully!" and no Retrofit wildcard parameter exceptions.
6. Commit changes to Git and push to GitHub `main`.
