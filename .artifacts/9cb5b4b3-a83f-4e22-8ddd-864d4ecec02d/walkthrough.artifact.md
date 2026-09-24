# 🚀 Walkthrough: Retrofit Wildcard Type Exception Fix

Resolved the `java.lang.IllegalArgumentException: Parameter type must not include a type variable or wildcard` when saving merchant product listings, menu items, or coupons.

---

## 🛠️ Summary of Changes

### 1. Retrofit Request Models & Interface
- Updated [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt):
  - Created strongly-typed Kotlin data classes:
    - `CreateProductRequest`
    - `CreateMenuItemRequest`
    - `CreateMerchantCouponRequest`
  - Updated Retrofit endpoints:
    - `addProduct(@Body request: CreateProductRequest)`
    - `addMenuItem(@Body request: CreateMenuItemRequest)`
    - `createMerchantCoupon(@Body request: CreateMerchantCouponRequest)`
    - `updateMerchantSettings(@Body request: @JvmSuppressWildcards Map<String, Any>)`
  - Replaced `@Body request: Map<String, Any>` which compiled to wildcard type `java.util.Map<String, ? extends Object>`, eliminating Retrofit Reflection parameter validation errors.

### 2. UI Screen Payloads
- Updated [AddEditProductScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/AddEditProductScreen.kt):
  - "Save Listing" action now constructs `CreateProductRequest` (for marketplace vendors) or `CreateMenuItemRequest` (for food kitchens).
- Updated [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt):
  - Coupon creation constructs `CreateMerchantCouponRequest`.

---

## 🧪 Device Verification & Deployment

- Built debug APK (`app:assembleDebug`) -> **`BUILD SUCCESSFUL`**.
- Installed and launched on connected Wireless ADB device (**Samsung Galaxy S23 Ultra** @ `192.168.1.2:42447`).
- Changes staged, committed (`7e57080e`), and pushed to GitHub `origin/main`.
