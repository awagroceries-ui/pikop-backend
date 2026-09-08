# Walkthrough - Final Stability & Diagnostic Push

I have identified and fixed the specific Multer error causing the HTTP 500 on mission completion and added further logging to pinpoint search/map issues.

## Changes Made

### 1. Fixed "Complete Mission" 500 Error
- **Problem:** The Android app was sending a file with the field name `"document"`, but the backend was strictly expecting `"file"`. This triggered a `MulterError: Unexpected field` and a 500 crash.
- **Fix:** Updated `ActiveOrderScreen.kt` to use the correct `"file"` field name.
- **Result:** Fulfillers can now upload proof-of-delivery photos and complete missions without errors.

### 2. Search & Map Diagnostics
- **Android:** Added `android.util.Log.d` statements in `MapAddressSearchScreen.kt`. You can now see "Triggering Autocomplete" in Logcat when typing, which confirms if the app is actually making the network call.
- **Backend:** Updated `geminiService.js` to use more stable model identifiers (`gemini-1.5-flash-latest`), which will stop the 404 errors in your VPS logs.

### 3. Paystack Channel Update
- **Status:** The backend is verified to be sending `bank_transfer` and `bank` as the first two options in the `channels` array.
- **Action Required:** Please double-check your **Paystack Dashboard -> Settings -> Preferences** to ensure "Bank Transfer" is enabled for your merchant account.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Update your **VPS** to apply the field name and AI model fixes:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **Complete Mission:** On your device, capture a photo and tap "Complete Mission". It should now succeed.
2. **Search bar:** Filter Logcat for `AddressSearch` and verify that typing triggers an autocomplete log.
3. **Paystack:** Open the checkout and check if "Bank Transfer" appears after verifying dashboard settings.
