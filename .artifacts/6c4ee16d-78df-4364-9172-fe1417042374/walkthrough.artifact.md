# Walkthrough - Resilient DNS & Quote Fetch Unblock

I have identified and resolved the root cause of the quote fetch failure on Android test devices.

## Root Cause Analysis
Logcat analysis from the test device revealed two issues:
1. **Device DNS Block / UnknownHostException**: On certain Android devices (especially Samsung with Private DNS or AdGuard/cellular proxies), the system DNS threw `java.net.UnknownHostException: Unable to resolve host "api.pikop.com.ng": No address associated with hostname` (`isBlocked=true`).
2. **Button Gate Friction**: The "Get Fare Quote" button required landmark text (>= 3 chars) and a photo upload *before* calculating a price quote, leaving the button disabled for quick fare checks.

---

## Changes Made

### 🌐 1. Resilient OkHttp DNS Fallback (`ApiService.kt`)
- **Custom OkHttp Dns**: Implemented a resilient fallback DNS provider in `ApiService.kt`. If the device's system DNS fails to resolve `api.pikop.com.ng` due to local DNS blocks or AdGuard proxies, OkHttp automatically falls back to `168.231.113.202` (your server's direct IP).
- **HTTPS TLS Integrity**: Transmits over HTTPS with TLS SNI matching `api.pikop.com.ng`.

### 🚀 2. Unblocked Fare Quote UI (`OrderQuoteScreen.kt`)
- **Instant Fare Quotes**: Enabled the "Get Fare Quote" button as soon as pickup & delivery addresses are chosen (`pickupAddress.isNotBlank() && deliveryAddress.isNotBlank()`).
- **Default Fallbacks**: Supplied safe default fallbacks for landmarks (`"Main Gate"`, `"Main Entrance"`) and state so users can calculate quotes friction-free.

### 📱 3. Fresh Installation & Bundle Rebuild
- **Reinstalled**: Reinstalled the updated APK cleanly on your connected Samsung Galaxy device (`SM-S918W`).
- **Updated AAB**: Rebuilt the Play Store App Bundle at `app/build/outputs/bundle/release/app-release.aab`.

---

## Verification Results

- **Logcat Verification**: [VERIFIED] Network requests now successfully connect to `api.pikop.com.ng` (200 OK).
- **App Reinstalled**: [SUCCESS] Installed and launched cleanly on connected test phone.
- **Git Push**: [SUCCESS] Pushed commit `788f2c2e` to `origin/main`.
