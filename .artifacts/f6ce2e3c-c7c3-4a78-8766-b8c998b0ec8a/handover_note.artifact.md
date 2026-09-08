# Comprehensive Handover Note: Project Pikop Logistics & Admin V3

This handover note serves as the definitive reference for a new chat assistant to take over Project Pikop without losing context, history, architecture, or configuration details.

---

## 🚀 1. Project Overview & Architecture

- **Project Name**: Pikop Logistics (Android App & Backend API v3).
- **Core Architecture**:
  - **Android Client (`/app`)**: Jetpack Compose, Material 3, Hilt (DI), Retrofit, Google Maps SDK, Coroutines.
  - **Backend API (`backend_v3`)**: Node.js, Express, PostgreSQL, Socket.io, EJS Admin views, Axios, Paystack, Google Places API.
  - **Production Server**: Ubuntu VPS (`srv1932412`), running via PM2 (`pikop-v3`), URL: `https://api.pikop.com.ng/`.

---

## 📊 2. Progress History & Current Status

### What Works Successfully ✅
1. **Build & Stability**: Android app builds successfully (`BUILD SUCCESSFUL`) with AGP 8.7.2, Gradle 8.13, Kotlin 2.0.21, and Hilt/KSP.
2. **KYC Completion UX**: Updated `KycUploadScreen.kt` to show a clear "Verification Completed!" notice guiding users back home to await admin approval.
3. **Admin Dashboard Core**:
   - **Mission Control & Live Orders**: View missions, track status.
   - **Fleet Directory (`/admin/fulfillers`)**: List, suspend, reactivate, and terminate fulfillers.
   - **Manual Order Override (`/admin/orders/:id/update`)**: Force-complete stuck orders or cancel/reassign missions.
   - **Transaction Audit Ledger (`/admin/transactions`)**: Complete ledger of wallet top-ups, settlements, escrow holds/releases, and payouts for dispute resolution.
   - **Customer Management (`/admin/customers`)**: Customer directory, account status, and wallet balance inspection.
4. **Payment Initialization**: Wallet top-ups and order checkout initialization (omitting restrictive `channels` so Paystack presents all enabled merchant channels).

### Current Challenges & Areas Needing Verification ⚠️
1. **Map Search Autocomplete**: Relies on backend proxy (`/api/v1/places/autocomplete`) calling Google Places API. Requires a valid Google API key with 35 APIs enabled in VPS `.env`.
2. **Paystack Checkout Channels**: Paystack rendering options depend on merchant account dashboard settings and channel configurations.

---

## 🔑 3. Working Configurations, Keys & `.env` Details

### Android (`local.properties` in project root)
```properties
sdk.dir=C\:\\Users\\MOSES\\AppData\\Local\\Android\\Sdk
googleMapsApiKey=AIzaSyDwCHF7qF7IjfMLnqxTl1YWjDjxPogu9RM
paystackPublicKey=pk_live_...
```

### Backend VPS (`/var/www/pikop-api/backend_v3/backend_v3/.env`)
```env
PORT=3000
NODE_ENV=production
DATABASE_URL=postgres://...
PAYSTACK_SECRET_KEY=sk_live_...
GOOGLE_MAPS_API_KEY=AIzaSyDwCHF7qF7IjfMLnqxTl1YWjDjxPogu9RM
GOOGLE_PLACES_API_KEY=AIzaSyDwCHF7qF7IjfMLnqxTl1YWjDjxPogu9RM
```
*(Note: Using the same working Maps key with 35 APIs enabled for both `GOOGLE_MAPS_API_KEY` and `GOOGLE_PLACES_API_KEY` ensures Google Cloud properly authenticates autocomplete proxy requests).*

---

## 🤖 4. Automation Instructions for the New Assistant

### Git Automation Policy
- **Always carry out all Git tasks automatically** whenever code changes are made:
  ```bash
  git add .
  git commit -m "<Descriptive commit message>"
  git push origin main
  ```

### VPS Deployment Instructions
When backend changes are pushed to GitHub, provide these exact commands for the user to execute on the VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

### Android Deployment & Testing via ADB
To test builds on a connected physical device or emulator:
```bash
./gradlew assembleDebug --no-daemon
adb uninstall com.ng.pikop
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ng.pikop/.MainActivity
```
