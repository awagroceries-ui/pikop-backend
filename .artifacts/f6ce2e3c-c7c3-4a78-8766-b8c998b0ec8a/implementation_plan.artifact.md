# Implementation Plan - Emergency Fix for 500 Error & Final Search/Map Diagnostics

This plan fixes the verified "Unexpected field" error causing mission completion failures and adds critical debugging for the map/search issues.

## Proposed Changes

### Android App

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Fix the `MultipartBody` field name from `"document"` to `"file"` to match the backend's Multer configuration.
- This will resolve the `MulterError: Unexpected field` and the resulting HTTP 500.

#### [MODIFY] [MapAddressSearchScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/MapAddressSearchScreen.kt)
- Add explicit logging before calling `apiService.getAutocomplete`.
- Print the exact URL being constructed to ensure the `BASE_URL` is correct.

---

### Backend (`backend_v3`)

#### [MODIFY] [orderRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/orderRoutes.js)
- Add a safety check/fallback for the Multer field name just in case.

#### [MODIFY] [geminiService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/geminiService.js)
- Update model names and versions to prevent the 404 errors seen in logs.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1. **Complete Mission:** Attempt to complete a mission. It should now proceed to the rating dialog without a 500 error.
2. **Search bar:** Open Logcat and filter for `AddressSearch`. Verify that `getAutocomplete` is being triggered when typing.
3. **Paystack:** **IMPORTANT:** Please verify in your **Paystack Dashboard -> Settings -> Preferences** that "Bank Transfer" is checked under "Payment Channels". If it is checked and still not appearing, we may need to contact Paystack support as the backend is correctly requesting it.
